package io.github.takusan23.akaricore.graphics.mediacodec

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.common.MediaExtractorTool
import kotlinx.coroutines.yield

/**
 * 動画のデコーダー
 * 目的としては動画をデコードして、[io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture]を出力先にし、[io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor]で描画する。
 * これ以外の目的でも（単に動画を再生する）でも使えるかも。
 */
class AkariVideoDecoder {

    private var decodeMediaCodec: MediaCodec? = null
    private var mediaExtractor: MediaExtractor? = null

    /** 最後の[seekTo]で取得したフレームの位置 */
    private var latestDecodePositionMs = 0L

    /** 前回のシーク位置 */
    private var prevSeekToMs = -1L

    /** 動画の時間（ミリ秒）。[prepare]を呼び出した後利用できます。 */
    var videoDurationMs: Long = -1
        private set

    /** 動画の縦のサイズ。[prepare]を呼び出した後利用できます。 */
    var videoHeight: Int = -1
        private set

    /** 動画の横のサイズ。[prepare]を呼び出した後利用できます。 */
    var videoWidth: Int = -1
        private set

    /**
     * デコーダーの準備をする
     *
     * @param input 再生する動画のファイル
     * @param outputSurface 映像フレームの出力先
     * @param isSdrToneMapping 10-bit HDR の動画を SDR に変換したい場合は true。トーンマッピングと呼ばれているものです。すべての端末で利用できるわけではないようです。
     */
    suspend fun prepare(
        input: AkariCoreInputOutput.Input,
        outputSurface: Surface,
        isSdrToneMapping: Boolean = false
    ) {
        val (mediaExtractor, index, mediaFormat) = MediaExtractorTool.extractMedia(input, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)!!
        this.mediaExtractor = mediaExtractor
        mediaExtractor.selectTrack(index)

        // ミリ秒に
        videoDurationMs = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1_000
        val _videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val _videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
        val sizePair = when (runCatching { mediaFormat.getInteger(MEDIA_FORMAT_KEY_ROTATE) }.getOrNull() ?: 0) {
            90, 270 -> _videoHeight to _videoWidth
            else -> _videoWidth to _videoHeight
        }
        videoWidth = sizePair.first
        videoHeight = sizePair.second

        // HDR を SDR にする場合（トーンマッピングする場合）
        if (isSdrToneMapping && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER_REQUEST, MediaFormat.COLOR_TRANSFER_SDR_VIDEO)
        }

        val codecName = mediaFormat.getString(MediaFormat.KEY_MIME)!!
        decodeMediaCodec = MediaCodec.createDecoderByType(codecName).apply {
            configure(mediaFormat, outputSurface, null, 0)
        }
        decodeMediaCodec?.start()
    }

    @Deprecated("seekTo() を使ってください") // TODO 次消す
    suspend fun _seekTo(seekToMs: Long): Boolean = seekTo(seekToMs).isNewFrame

    /**
     * シークする。
     * これを連続で呼び出しフレームを連続で取り出し再生させる。
     *
     * @param seekToMs 動画フレームの時間
     * @return [SeekResult]
     */
    suspend fun seekTo(seekToMs: Long): SeekResult {
        val isSuccessDecodeFrame = when {
            // 現在の再生位置よりも戻る方向に（巻き戻し）した場合
            seekToMs < prevSeekToMs -> {
                latestDecodePositionMs = prevSeekTo(seekToMs)
                SeekResult(
                    isSuccessful = true,
                    isNewFrame = true
                )
            }

            // シーク不要
            // 例えば 30fps なら 33ms 毎なら新しい Bitmap を返す必要があるが、 16ms 毎に要求されたら Bitmap 変化しないので
            // つまり映像のフレームレートよりも高頻度で Bitmap が要求されたら、前回取得した Bitmap がそのまま使い回せる
            seekToMs < latestDecodePositionMs -> {
                // do nothing
                SeekResult(
                    isSuccessful = true,
                    isNewFrame = false
                )
            }

            else -> {
                // 巻き戻しでも無く、フレームを取り出す必要がある
                val framePositionMsOrNull = nextSeekTo(seekToMs)
                if (framePositionMsOrNull != null) {
                    latestDecodePositionMs = framePositionMsOrNull
                }
                SeekResult(
                    isSuccessful = framePositionMsOrNull != null,
                    isNewFrame = true
                )
            }
        }
        prevSeekToMs = seekToMs
        return isSuccessDecodeFrame
    }

    /** 破棄する */
    fun destroy() {
        decodeMediaCodec?.stop()
        decodeMediaCodec?.release()
        mediaExtractor?.release()
    }

    /**
     * 前回の時間よりも次のフレームを取り出す。
     * シークするとキーフレームまで戻ってしまうので、極力シークを避けるようにしています。
     *
     * @param seekToMs 欲しいフレームの時間
     * @return 次のフレームがない場合は null。そうじゃない場合は動画フレームの時間
     */
    private suspend fun nextSeekTo(seekToMs: Long): Long? {
        val decodeMediaCodec = decodeMediaCodec!!
        val mediaExtractor = mediaExtractor!!

        // はみ出したら null
        // MediaExtractor の時間で見ると、もう先にデコーダーに入れた時間が帰ってきてしまう（デコードはまだ）
        if (videoDurationMs < seekToMs) {
            return null
        }

        var isRunning = true
        val bufferInfo = MediaCodec.BufferInfo()
        var returnValue: Long? = null
        while (isRunning) {

            // キャンセル時
            yield()

            // コンテナフォーマットからサンプルを取り出し、デコーダーに渡す
            // シークしないことで、連続してフレームを取得する場合にキーフレームまで戻る必要がなくなり、早くなる
            val inputBufferIndex = decodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (0 <= inputBufferIndex) {
                // デコーダーへ流す
                val inputBuffer = decodeMediaCodec.getInputBuffer(inputBufferIndex)!!
                val size = mediaExtractor.readSampleData(inputBuffer, 0)
                // データが有ればデコーダーへ、もうデータがなければ終了シグナルを送る
                if (0 <= size) {
                    decodeMediaCodec.queueInputBuffer(inputBufferIndex, 0, size, mediaExtractor.sampleTime, 0)
                    mediaExtractor.advance()

                    // シーク先が、果てしなく遠い場所になっているときの対応
                    // 極力シークしないことで、高速にフレームを取り出せるようにしている（シークすると戻る必要が出てくるので）
                    // が、動画編集で、動画素材の最後をプレビューする際に、一切シークがないと遅くなってしまう
                    // なので、果てしなく遠い、つまり、連続で取り出して先にキーフレームが来た場合、その場合は、近場のキーフレームにシークしたほうが速いので
                    val isAvailable = mediaExtractor.sampleTime != -1L
                    val isKeyframe = mediaExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
                    val extractTimeMs = mediaExtractor.sampleTime / 1000L
                    if (isAvailable && isKeyframe && extractTimeMs < seekToMs) {
                        mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                        decodeMediaCodec.flush()
                    }

                } else {
                    decodeMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
            }

            // デコーダーから映像を受け取る部分
            var isDecoderOutputAvailable = true
            while (isDecoderOutputAvailable) {

                // キャンセル時
                yield()

                // デコード結果が来ているか
                val outputBufferIndex = decodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // リトライが必要
                        isDecoderOutputAvailable = false
                    }

                    0 <= outputBufferIndex -> {
                        // ImageReader ( Surface ) に描画する
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)

                        // もうデコーダーからデータが来ない場合はループを抜ける
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            isRunning = false
                            isDecoderOutputAvailable = false
                            returnValue = null
                        }

                        if (doRender) {
                            // 欲しいフレームの時間に到達した場合、ループを抜ける
                            val presentationTimeMs = bufferInfo.presentationTimeUs / 1000
                            if (seekToMs <= presentationTimeMs) {
                                isRunning = false
                                isDecoderOutputAvailable = false
                                returnValue = presentationTimeMs
                            }
                        }
                    }
                }
            }
        }

        return returnValue
    }

    /**
     * 前回の時間よりも前のフレームを取り出す。
     * キーフレームまで戻るため[nextSeekTo]より時間がかかります。
     *
     * @param seekToMs 欲しいフレームの時間
     * @return フレームの時間
     */
    private suspend fun prevSeekTo(seekToMs: Long): Long {
        val decodeMediaCodec = decodeMediaCodec!!
        val mediaExtractor = mediaExtractor!!

        // シークする。SEEK_TO_PREVIOUS_SYNC なので、シーク位置にキーフレームがない場合はキーフレームがある場所まで戻る
        // エンコードサれたデータを順番通りに送るわけではない（隣接したデータじゃない）ので flush する
        mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        decodeMediaCodec.flush()

        // デコーダーに渡す
        var isRunning = true
        val bufferInfo = MediaCodec.BufferInfo()
        var returnValue = 0L
        while (isRunning) {
            // キャンセル時
            yield()

            // コンテナフォーマットからサンプルを取り出し、デコーダーに渡す
            // while で繰り返しているのは、シーク位置がキーフレームのため戻った場合に、狙った時間のフレームが表示されるまで繰り返しデコーダーに渡すため
            val inputBufferIndex = decodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (0 <= inputBufferIndex) {
                val inputBuffer = decodeMediaCodec.getInputBuffer(inputBufferIndex)!!
                // デコーダーへ流す
                val size = mediaExtractor.readSampleData(inputBuffer, 0)
                decodeMediaCodec.queueInputBuffer(inputBufferIndex, 0, size, mediaExtractor.sampleTime, 0)
                // 狙ったフレームになるまでデータを進める
                mediaExtractor.advance()
            }

            // デコーダーから映像を受け取る部分
            var isDecoderOutputAvailable = true
            while (isDecoderOutputAvailable) {
                // デコード結果が来ているか
                val outputBufferIndex = decodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // リトライが必要
                        isDecoderOutputAvailable = false
                    }

                    0 <= outputBufferIndex -> {
                        // Surface へ描画
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)
                        // 欲しいフレームの時間に到達した場合、ループを抜ける
                        val presentationTimeMs = bufferInfo.presentationTimeUs / 1000
                        if (doRender) {
                            if (seekToMs <= presentationTimeMs) {
                                isRunning = false
                                isDecoderOutputAvailable = false
                                returnValue = presentationTimeMs
                            }
                        }
                    }
                }
            }

            // もうない場合
            if (mediaExtractor.sampleTime == -1L) break
        }

        return returnValue
    }

    /**
     * [seekTo]の結果
     *
     * @param isSuccessful フレームが取得できた場合は true。もう無い場合などは false。
     * @param isNewFrame フレームが更新された場合は true。つまり動画の fps よりも早くフレームを取り出した場合、前回のフレームが使われることがあるため、その場合は false。
     */
    data class SeekResult internal constructor(
        val isSuccessful: Boolean,
        val isNewFrame: Boolean
    )

    companion object {
        /** MediaCodec タイムアウト */
        private const val TIMEOUT_US = 0L

        /** 動画の回転情報。縦動画の場合は 90 って入っている */
        private const val MEDIA_FORMAT_KEY_ROTATE = "rotation-degrees"
    }

}