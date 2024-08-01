package io.github.takusan23.akaricore.graphics

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.common.MediaExtractorTool
import io.github.takusan23.akaricore.video.MediaParserKeyFrameTimeDetector
import io.github.takusan23.akaricore.video.getImageReaderBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class AkariGraphicsVideoTexture(initTexName: Int) {

    /** 映像データの送信先 SurfaceTexture */
    val akariSurfaceTexture = AkariGraphicsSurfaceTexture(initTexName)

    /** MediaCodec デコーダー */
    private var decodeMediaCodec: MediaCodec? = null

    /** Extractor */
    private var mediaExtractor: MediaExtractor? = null

    /** 最後の[seekDecoderPosition]で取得したフレームの位置 */
    private var latestDecodePositionMs = 0L

    /** 前回のシーク位置 */
    private var prevSeekToMs = -1L

    /** コンテナフォーマットをパースして、キーフレームの時間を探してくれるやつ */
    private var mediaParserKeyFrameTimeDetector: MediaParserKeyFrameTimeDetector? = null

    var videoWidth: Int = -1
    var videoHeight: Int = -1

    /**
     * デコーダーを初期化する
     * クロマキー機能を利用しない場合は、[chromakeyThreshold]、[chromakeyColor]は null でいいです。
     *
     * @param input 動画ファイル。詳しくは[AkariCoreInputOutput.Input]
     * @param chromakeyThreshold クロマキーのしきい値。
     * @param chromakeyColor クロマキーの色。しきい値を考慮するので、近しい色も透過するはず。
     */
    suspend fun prepareDecoder(
        input: AkariCoreInputOutput.Input,
        // TODO クロマキー
        chromakeyThreshold: Float? = null,
        chromakeyColor: Int? = null
    ) {
        val (mediaExtractor, index, mediaFormat) = MediaExtractorTool.extractMedia(input, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)!!
        this@AkariGraphicsVideoTexture.mediaExtractor = mediaExtractor
        mediaExtractor.selectTrack(index)

        val codecName = mediaFormat.getString(MediaFormat.KEY_MIME)!!
        videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
        videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)

        // TODO 不要なら消す
        // akariSurfaceTexture.setTextureSize(videoWidth, videoHeight)

        // 映像デコーダー起動
        decodeMediaCodec = MediaCodec.createDecoderByType(codecName).apply {
            configure(mediaFormat, akariSurfaceTexture.surface, null, 0)
        }
        decodeMediaCodec!!.start()

        // パースする
        // TODO 十分速い場合はいらないかも
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 変なコンテナフォーマット来たら落ちる
            // 落ちないようにする
            mediaParserKeyFrameTimeDetector = try {
                MediaParserKeyFrameTimeDetector(
                    onCreateInputStream = { input.inputStream() }
                ).apply { startParse() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    /** デコーダーを破棄する */
    fun destroy() {
        decodeMediaCodec?.release()
        mediaExtractor?.release()
        akariSurfaceTexture.destroy()
    }

    suspend fun drawLoop() = withContext(Dispatchers.Default) {
        val decodeMediaCodec = decodeMediaCodec!!
        val mediaExtractor = mediaExtractor!!

        // advance() で false を返したことがある場合、もうデータがない。getSampleTime も -1 になる。
        if (mediaExtractor.sampleTime == -1L) {
            return@withContext false
        }

        var isRunning = isActive
        val bufferInfo = MediaCodec.BufferInfo()
        while (isRunning) {
            // キャンセル時
            if (!isActive) break

            // コンテナフォーマットからサンプルを取り出し、デコーダーに渡す
            // シークしないことで、連続してフレームを取得する場合にキーフレームまで戻る必要がなくなり、早くなる
            val inputBufferIndex = decodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                // デコーダーへ流す
                val inputBuffer = decodeMediaCodec.getInputBuffer(inputBufferIndex)!!
                val size = mediaExtractor.readSampleData(inputBuffer, 0)
                decodeMediaCodec.queueInputBuffer(inputBufferIndex, 0, size, mediaExtractor.sampleTime, 0)
            }

            // デコード結果が来ているか
            while (isActive) {
                val outputBufferIndex = decodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        break
                    }

                    outputBufferIndex >= 0 -> {
                        // SurfaceTexture に描画して、ループを抜ける
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, true)
                    }
                }
            }

            // 次に進める。advance() が false の場合はもうデータがないので、break する。
            val isEndOfFile = !mediaExtractor.advance()
            if (isEndOfFile) {
                // return で false（フレームが取得できない旨）を返す
                break
            }
        }
    }

    /** 指定位置の動画フレームを[AkariGraphicsTextureRenderer]へ描画する */
    suspend fun draw(
        akariGraphicsTextureRenderer: AkariGraphicsTextureRenderer,
        seekToMs: Long,
        onTransform: ((mvpMatrix: FloatArray) -> Unit)? = null
    ) {
        // シークする
        seekDecoderPosition(seekToMs)
        prevSeekToMs = seekToMs
        // AkariGraphicsTextureRenderer に描画する。高速な OpenGL の恩恵を受ける
        akariGraphicsTextureRenderer.drawSurfaceTexture(
            akariSurfaceTexture = akariSurfaceTexture,
            isAwaitTextureUpdate = false,
            onTransform = onTransform
        )
    }

    /** 前か後ろ、もしくはシーク不要なら何もしない。 */
    suspend fun seekDecoderPosition(seekToMs: Long) {
        when {
            // 現在の再生位置よりも戻る方向に（巻き戻し）した場合
            seekToMs < prevSeekToMs -> {
                println("awaitSeekToPrevDecode")
                awaitSeekToPrevDecode(seekToMs)
            }

            // シーク不要
            // 例えば 30fps なら 33ms 毎なら新しい Bitmap を返す必要があるが、 16ms 毎に要求されたら Bitmap 変化しないので
            // つまり映像のフレームレートよりも高頻度で Bitmap が要求されたら、前回取得した Bitmap がそのまま使い回せる
            seekToMs < latestDecodePositionMs -> {
                // do nothing
                println("seekToMs < latestDecodePositionMs")
            }

            // 次のフレームに移動
            else -> {
                println("awaitSeekToNextDecode()")
                awaitSeekToNextDecode(seekToMs)
            }
        }
    }

    /**
     * 今の再生位置よりも後の位置にシークして、指定した時間のフレームまでデコードする。
     *
     * また高速化のため、まず[seekToMs]へシークするのではなく、次のキーフレームまでデータをデコーダーへ渡します。
     * この間に[seekToMs]のフレームがあればシークしません。
     * これにより、キーフレームまで戻る必要がなくなり、連続してフレームを取得する場合は高速に取得できます。
     *
     * @param seekToMs シーク位置
     * @return もうデータがない場合は false。データが有り[getImageReaderBitmap]が呼び出せる場合は true。
     */
    suspend fun awaitSeekToNextDecode(
        seekToMs: Long
    ): Boolean = coroutineScope {
        val decodeMediaCodec = decodeMediaCodec!!
        val mediaExtractor = mediaExtractor!!

        // advance() で false を返したことがある場合、もうデータがない。getSampleTime も -1 になる。
        if (mediaExtractor.sampleTime == -1L) {
            return@coroutineScope false
        }

        // Android 11 以降は MediaParser クラスが使えるので分岐
        // Android 11 以降、コンテナフォーマットを解析するクラスが登場し、これのお陰で、キーフレームの位置を取得できるように
        // このため、欲しいフレームが数フレーム先なのでシークせずに待つか、はたまたずっと先の場合はここでシークしてしまう
        // Android 10 以前は使えないので、次のキーフレームが来るまではシークしない
        if (mediaParserKeyFrameTimeDetector != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val seekToUs = seekToMs * 1_000
            val sampleTime = mediaExtractor.sampleTime
            val currentPositionPrevKeyFramePositionUs = mediaParserKeyFrameTimeDetector!!.getPrevKeyFrameTime(sampleTime)
            val seekToPositionPrevKeyFramePositionUs = mediaParserKeyFrameTimeDetector!!.getPrevKeyFrameTime(seekToUs)

            // null なら何もしない
            if (currentPositionPrevKeyFramePositionUs != null && seekToPositionPrevKeyFramePositionUs != null) {
                // 次のキーフレームよりも前に欲しいフレームがあればシークしない
                // 現在位置に近いキーフレームよりも先に、欲しいフレームに近いキーフレームがある場合のみシーク
                // 次のキーフレームまでに欲しいフレームがあれば while で来るまで待つ
                if (currentPositionPrevKeyFramePositionUs < seekToPositionPrevKeyFramePositionUs) {
                    mediaExtractor.seekTo(seekToUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    decodeMediaCodec.flush()
                }
            }
        }

        var returnValue = true
        var isRunning = isActive
        val bufferInfo = MediaCodec.BufferInfo()
        while (isRunning) {
            // キャンセル時
            if (!isActive) break

            // コンテナフォーマットからサンプルを取り出し、デコーダーに渡す
            // シークしないことで、連続してフレームを取得する場合にキーフレームまで戻る必要がなくなり、早くなる
            val inputBufferIndex = decodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                // デコーダーへ流す
                val inputBuffer = decodeMediaCodec.getInputBuffer(inputBufferIndex)!!
                val size = mediaExtractor.readSampleData(inputBuffer, 0)
                decodeMediaCodec.queueInputBuffer(inputBufferIndex, 0, size, mediaExtractor.sampleTime, 0)
            }

            // デコーダーから映像を受け取る部分
            var isDecoderOutputAvailable = true
            while (isDecoderOutputAvailable) {
                // キャンセル時
                if (!isActive) break

                // デコード結果が来ているか
                val outputBufferIndex = decodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // もう無い時
                        isDecoderOutputAvailable = false
                    }

                    outputBufferIndex >= 0 -> {
                        // SurfaceTexture に描画して、ループを抜ける
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)
                        if (doRender) {
                            // 欲しいフレームの時間に到達した場合、ループを抜ける
                            // doRender == true じゃないと ImageReader から取り出せないので
                            val presentationTimeMs = bufferInfo.presentationTimeUs / 1000
                            if (seekToMs <= presentationTimeMs) {
                                isRunning = false
                                latestDecodePositionMs = presentationTimeMs
                            }
                        }
                    }
                }
            }

            // 次に進める。advance() が false の場合はもうデータがないので、break する。
            val isEndOfFile = !mediaExtractor.advance()
            if (isEndOfFile) {
                // return で false（フレームが取得できない旨）を返す
                returnValue = false
                break
            }

            // 欲しいフレームが前回の呼び出しと連続していないときの処理。
            // Android 10 以前はここでシークの判断をします。Android 11 以降は MediaParserKeyFrameTimeDetector でシークの判断をします。
            // 例えば、前回の取得位置よりもさらに数秒以上先にシークした場合、指定位置になるまで待ってたら遅くなるので、数秒先にあるキーフレームまでシークする
            // で、このシークが必要かどうかの判定がこれ。数秒先をリクエストした結果、欲しいフレームが来るよりも先にキーフレームが来てしまった
            // この場合は一気にシーク位置に一番近いキーフレームまで進める
            // ただし、キーフレームが来ているサンプルの時間を比べて、欲しいフレームの位置の方が大きくなっていることを確認してから。
            // デコーダーの時間 presentationTimeUs と、MediaExtractor の sampleTime は同じじゃない？らしく、sampleTime の方がデコーダーの時間より早くなるので注意
            val isKeyFrame = mediaExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
            val currentSampleTimeMs = mediaExtractor.sampleTime / 1000
            if (isKeyFrame && currentSampleTimeMs < seekToMs) {
                mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                decodeMediaCodec.flush()
            }
        }

        return@coroutineScope returnValue
    }

    /**
     * 今の再生位置よりも前の位置にシークして、指定した時間のフレームまでデコードする。
     * 指定した時間のフレームがキーフレームじゃない場合は、キーフレームまでさらに巻き戻すので、ちょっと時間がかかります。
     *
     * @param seekToMs シーク位置
     */
    private suspend fun awaitSeekToPrevDecode(
        seekToMs: Long
    ) = withContext(Dispatchers.Default) {
        val decodeMediaCodec = decodeMediaCodec!!
        val mediaExtractor = mediaExtractor!!

        // シークする。SEEK_TO_PREVIOUS_SYNC なので、シーク位置にキーフレームがない場合はキーフレームがある場所まで戻る
        mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        // エンコードサれたデータを順番通りに送るわけではない（隣接したデータじゃない）ので flush する
        decodeMediaCodec.flush()

        // デコーダーに渡す
        var isRunning = true
        val bufferInfo = MediaCodec.BufferInfo()
        while (isRunning) {
            // キャンセル時
            if (!isActive) break

            // コンテナフォーマットからサンプルを取り出し、デコーダーに渡す
            // while で繰り返しているのは、シーク位置がキーフレームのため戻った場合に、狙った時間のフレームが表示されるまで繰り返しデコーダーに渡すため
            val inputBufferIndex = decodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
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
                        // もう無い時
                        isDecoderOutputAvailable = false
                    }

                    outputBufferIndex >= 0 -> {
                        // SurfaceTexture に描画して、ループを抜ける
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)
                        if (doRender) {
                            // 欲しいフレームの時間に到達した場合、ループを抜ける
                            // doRender == true じゃないと ImageReader から取り出せないので
                            val presentationTimeMs = bufferInfo.presentationTimeUs / 1000
                            if (seekToMs <= presentationTimeMs) {
                                isRunning = false
                                latestDecodePositionMs = presentationTimeMs
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** MediaCodec タイムアウト */
        // TODO 非同期モードのがデコードが早い理由は、このタイムアウトが長すぎるせい、10_000 じゃなくて極端に 1_000 とかにすれば非同期と同じくらいの速度が出る
        // TODO ただ、SurfaceTexture に転送と、OpenGL ES が描画を同じスレッドと言うか、転送後にすぐ OpenGL で描画しない場合（スレッドが別とかで）、同期モードでタイムアウトが速すぎると乱れてしまった。非同期だとなぜか起きない。
        private const val TIMEOUT_US = 1_000L
    }

}