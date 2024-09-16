package io.github.takusan23.akaricore.video

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.common.MediaExtractorTool
import io.github.takusan23.akaricore.video.gl.FrameExtractorRenderer
import io.github.takusan23.akaricore.video.gl.InputSurface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * [android.media.MediaMetadataRetriever.getFrameAtTime]が遅いので、[MediaCodec]あたりを使って高速に[Bitmap]を返すやつを作る。
 * また、[android.media.MediaMetadataRetriever]を複数用意して、[Bitmap]を作ろうとしても何故か速度が変わらない（共有している・・？）
 */
@OptIn(DelicateCoroutinesApi::class)
class VideoFrameBitmapExtractor {

    /** OpenGL 用に用意した描画用スレッド。Kotlin coroutines では Dispatcher を切り替えて使う */
    private val openGlRendererThreadDispatcher = newSingleThreadContext("openGlRendererThreadDispatcher")

    /** MediaCodec デコーダー */
    private var decodeMediaCodec: MediaCodec? = null

    /** Extractor */
    private var mediaExtractor: MediaExtractor? = null

    /** 映像デコーダーから Bitmap として取り出すための ImageReader */
    private var imageReader: ImageReader? = null

    /** MediaCodec と OpenGL ES と繋ぐやつ */
    private var inputSurface: InputSurface? = null

    /** MediaCodec でフレームを受け取って、OpenGL で描画するやつ */
    private var frameExtractorRenderer: FrameExtractorRenderer? = null

    /** コンテナフォーマットをパースして、キーフレームの時間を探してくれるやつ */
    private var mediaParserKeyFrameTimeDetector: MediaParserKeyFrameTimeDetector? = null

    /** 最後の[getVideoFrameBitmap]で取得したフレームの位置 */
    private var latestDecodePositionMs = 0L

    /** 前回のシーク位置 */
    private var prevSeekToMs = -1L

    /** 前回[getImageReaderBitmap]で作成した Bitmap */
    private var prevBitmap: Bitmap? = null

    // 動画の縦横
    // どうやっても ImageReader でぶっ壊れた映像が出てくることがあるので（videoWidth = 1104 / videoHeight = 2560）、
    // 縦横同じサイズで ImageReader を作り、出てきた Bitmap を scale して戻す。
    private var videoHeight: Int = 0
    private var videoWidth: Int = 0

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
        chromakeyThreshold: Float? = null,
        chromakeyColor: Int? = null
    ) {
        val (mediaExtractor, index, mediaFormat) = MediaExtractorTool.extractMedia(input, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)!!
        this@VideoFrameBitmapExtractor.mediaExtractor = mediaExtractor
        mediaExtractor.selectTrack(index)

        val codecName = mediaFormat.getString(MediaFormat.KEY_MIME)!!
        videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
        videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)

        // 16 で割り切れる数字にする
        // Snapdragon も Google Tensor も 16 の倍数じゃないと動画のフレームが乱れてしまう
        // TODO nearestImageReaderAvailableSize が必要な場合の判定
        val fixWidth = videoWidth.toFixImageReaderSupportValue()
        val fixHeight = videoHeight.toFixImageReaderSupportValue()

        imageReader = ImageReader.newInstance(fixWidth, fixHeight, PixelFormat.RGBA_8888, 2)

        // OpenGL ES の用意
        // MediaCodec と ImageReader の間に OpenGL を経由させる
        // 経由させないと、Google Pixel 以外（Snapdragon 端末とか）で動かなかった
        inputSurface = InputSurface(outputSurface = imageReader!!.surface)
        // クロマキーするなら
        frameExtractorRenderer = FrameExtractorRenderer(
            chromakeyThreshold = chromakeyThreshold,
            chromakeyColor = chromakeyColor
        )

        // OpenGL の関数を呼ぶ際は、描画用スレッドに切り替えてから
        withContext(openGlRendererThreadDispatcher) {
            inputSurface?.makeCurrent()
            frameExtractorRenderer?.createRenderer()
        }

        // 映像デコーダー起動
        decodeMediaCodec = MediaCodec.createDecoderByType(codecName).apply {
            configure(mediaFormat, frameExtractorRenderer!!.inputSurface, null, 0)
        }
        decodeMediaCodec!!.start()

        // パースする
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
        imageReader?.close()
        inputSurface?.destroy()
        frameExtractorRenderer?.destroy()
        openGlRendererThreadDispatcher.close()
    }

    /**
     * 指定位置の動画のフレームを取得して、[Bitmap]で返す
     *
     * @param seekToMs シーク位置
     * @return Bitmap。もうデータがない場合は null。
     */
    suspend fun getVideoFrameBitmap(
        seekToMs: Long
    ): Bitmap? = withContext(Dispatchers.Default) {
        val videoFrameBitmap = when {
            // 現在の再生位置よりも戻る方向に（巻き戻し）した場合
            seekToMs < prevSeekToMs -> {
                awaitSeekToPrevDecode(seekToMs)
                getImageReaderBitmap()
            }

            // シーク不要
            // 例えば 30fps なら 33ms 毎なら新しい Bitmap を返す必要があるが、 16ms 毎に要求されたら Bitmap 変化しないので
            // つまり映像のフレームレートよりも高頻度で Bitmap が要求されたら、前回取得した Bitmap がそのまま使い回せる
            seekToMs < latestDecodePositionMs && prevBitmap != null -> {
                prevBitmap!!
            }

            else -> {
                // 巻き戻しでも無く、フレームを取り出す必要がある
                val hasData = awaitSeekToNextDecode(seekToMs)
                if (hasData) getImageReaderBitmap() else null
            }
        }
        prevSeekToMs = seekToMs
        return@withContext videoFrameBitmap
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
    private suspend fun awaitSeekToNextDecode(
        seekToMs: Long
    ): Boolean = withContext(Dispatchers.Default) {
        val decodeMediaCodec = decodeMediaCodec!!
        val mediaExtractor = mediaExtractor!!
        val inputSurface = inputSurface!!

        // advance() で false を返したことがある場合、もうデータがない。getSampleTime も -1 になる。
        if (mediaExtractor.sampleTime == -1L) {
            return@withContext false
        }

        // Android 11 以降は MediaParser クラスが使えるので分岐
        // Android 11 以降、コンテナフォーマットを解析するクラスが登場し、これのお陰で、キーフレームの位置を取得できるように
        // このため、欲しいフレームが数フレーム先なのでシークせずに待つか、はたまたずっと先の場合はここでシークしてしまう
        // Android 10 以前は使えないので、次のキーフレームが来るまではシークしない
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val seekToUs = seekToMs * 1_000
            val sampleTime = mediaExtractor.sampleTime
            val currentPositionPrevKeyFramePositionUs = mediaParserKeyFrameTimeDetector?.getPrevKeyFrameTime(sampleTime)
            val seekToPositionPrevKeyFramePositionUs = mediaParserKeyFrameTimeDetector?.getPrevKeyFrameTime(seekToUs)

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
            if (0 <= inputBufferIndex) {
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
                        // リトライが必要
                        isDecoderOutputAvailable = false
                    }

                    0 <= outputBufferIndex -> {
                        // ImageReader ( Surface ) に描画する
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)
                        // OpenGL で描画して、ImageReader で撮影する
                        // OpenGL 描画用スレッドに切り替えてから、swapBuffers とかやる
                        if (doRender) {
                            withContext(openGlRendererThreadDispatcher) {
                                frameExtractorRenderer?.draw()
                                inputSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000)
                                inputSurface.swapBuffers()
                            }
                            // 欲しいフレームの時間に到達した場合、ループを抜ける
                            // doRender == true じゃないと ImageReader から取り出せないので
                            val presentationTimeMs = bufferInfo.presentationTimeUs / 1000
                            if (seekToMs <= presentationTimeMs) {
                                isRunning = false
                                isDecoderOutputAvailable = false
                                latestDecodePositionMs = presentationTimeMs
                            }
                        }
                    }
                }
            }

            // 次に進める。デコーダーにデータを入れた事を確認してから。
            // advance() が false の場合はもうデータがないので、break
            if (0 <= inputBufferIndex) {
                val isEndOfFile = !mediaExtractor.advance()
                if (isEndOfFile) {
                    // return で false（フレームが取得できない旨）を返す
                    returnValue = false
                    break
                }
            }

            // 同様に
            if (0 <= inputBufferIndex) {
                // 欲しいフレームが前回の呼び出しと連続していないときの処理。
                // Android 10 以前はここでシークの判断をします。Android 11 以降は MediaParserKeyFrameTimeDetector でシークの判断をします。
                // 例えば、前回の取得位置よりもさらに数秒以上先にシークした場合、指定位置になるまで待ってたら遅くなるので、数秒先にあるキーフレームまでシークする
                // で、このシークが必要かどうかの判定がこれ。数秒先をリクエストした結果、欲しいフレームが来るよりも先にキーフレームが来てしまった
                // この場合は一気にシーク位置に一番近いキーフレームまで進める
                // ただし、キーフレームが来ているサンプルの時間を比べて、欲しいフレームの位置の方が大きくなっていることを確認してから。
                // デコーダーの時間 presentationTimeUs と、MediaExtractor の sampleTime は同じじゃない？らしく、sampleTime の方がデコーダーの時間より早くなるので注意
                val isKeyFrame = mediaExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
                val currentSampleTimeMs = mediaExtractor.sampleTime / 1000
//                println("loop bufferInfo.presentationTimeUs = ${bufferInfo.presentationTimeUs / 1000} / sampleTime = ${mediaExtractor.sampleTime / 1000} / isKeyFrame = ${isKeyFrame} / seekToMs = $seekToMs")
                if (isKeyFrame && currentSampleTimeMs < seekToMs) {
//                    println("mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)")
                    mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
//                    println("seekTo sampleTime = ${mediaExtractor.sampleTime / 1000}")
                    decodeMediaCodec.flush()
                }
            }
        }

        return@withContext returnValue
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
        val inputSurface = inputSurface!!

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
                        // ImageReader ( Surface ) に描画する
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)
                        // OpenGL で描画して、ImageReader で撮影する
                        // OpenGL 描画用スレッドに切り替えてから、swapBuffers とかやる
                        if (doRender) {
                            withContext(openGlRendererThreadDispatcher) {
                                frameExtractorRenderer?.draw()
                                inputSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000)
                                inputSurface.swapBuffers()
                            }
                            // 欲しいフレームの時間に到達した場合、ループを抜ける
                            // doRender == true じゃないと ImageReader から取り出せないので
                            val presentationTimeMs = bufferInfo.presentationTimeUs / 1000
                            if (seekToMs <= presentationTimeMs) {
                                isRunning = false
                                isDecoderOutputAvailable = false
                                latestDecodePositionMs = presentationTimeMs
                            }
                        }
                    }
                }
            }
        }
    }

    /** [imageReader]から[Bitmap]を取り出す */
    private suspend fun getImageReaderBitmap(): Bitmap {
        // ImageReader から取り出して、アスペクト比を戻す
        val fixAspectRateBitmap = imageReader!!.getImageReaderBitmap(videoWidth, videoHeight)
        prevBitmap = fixAspectRateBitmap
        return fixAspectRateBitmap
    }

    companion object {
        /** MediaCodec タイムアウト */
        private const val TIMEOUT_US = 0L
    }

}