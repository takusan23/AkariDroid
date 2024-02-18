package io.github.takusan23.akaricore.v2.video

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import io.github.takusan23.akaricore.v2.common.AkariCoreInputDataSource
import io.github.takusan23.akaricore.v2.common.MediaExtractorTool
import io.github.takusan23.akaricore.v2.video.gl.VideoFrameInputSurface
import io.github.takusan23.akaricore.v2.video.gl.VideoFrameTextureRenderer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

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

    /** MediaCodec でフレームを受け取って、OpenGL で描画するやつ */
    private var inputSurface: VideoFrameInputSurface? = null

    /** 最後の[getVideoFrameBitmap]で取得したフレームの位置 */
    private var latestDecodePositionMs = 0L

    /** 前回のシーク位置 */
    private var prevSeekToMs = -1L

    /** 前回[getImageReaderBitmap]で作成した Bitmap */
    private var prevBitmap: Bitmap? = null

    /**
     * デコーダーを初期化する
     *
     * @param akariCoreInputDataSource [AkariCoreInputDataSource]。Uri か File です。
     */
    suspend fun prepareDecoder(
        akariCoreInputDataSource: AkariCoreInputDataSource
    ) {
        val (mediaExtractor, index, mediaFormat) = when (akariCoreInputDataSource) {
            is AkariCoreInputDataSource.AndroidUri -> akariCoreInputDataSource.getFileDescriptor()
                .use { fd -> MediaExtractorTool.extractMedia(fd.fileDescriptor, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO) }

            is AkariCoreInputDataSource.JavaFile -> MediaExtractorTool.extractMedia(akariCoreInputDataSource.file.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)
        }!!
        this@VideoFrameBitmapExtractor.mediaExtractor = mediaExtractor
        mediaExtractor.selectTrack(index)

        val codecName = mediaFormat.getString(MediaFormat.KEY_MIME)!!
        val videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)

        // Surface 経由で Bitmap が取れる ImageReader つくる
        imageReader = ImageReader.newInstance(videoWidth, videoHeight, PixelFormat.RGBA_8888, 2)

        // OpenGL 描画用スレッドに切り替える
        withContext(openGlRendererThreadDispatcher) {
            // MediaCodec と ImageReader の間に OpenGL を経由させる
            // 経由させないと、Google Pixel 以外（Snapdragon 端末とか）で動かなかった
            this@VideoFrameBitmapExtractor.inputSurface = VideoFrameInputSurface(
                surface = imageReader!!.surface,
                textureRenderer = VideoFrameTextureRenderer()
            )
            inputSurface!!.makeCurrent()
            inputSurface!!.createRender()
        }

        // 映像デコーダー起動
        decodeMediaCodec = MediaCodec.createDecoderByType(codecName).apply {
            configure(mediaFormat, inputSurface!!.drawSurface, null, 0)
        }
        decodeMediaCodec!!.start()
    }

    /** デコーダーを破棄する */
    fun destroy() {
        decodeMediaCodec?.release()
        mediaExtractor?.release()
        imageReader?.close()
        inputSurface?.release()
        openGlRendererThreadDispatcher.close()
    }

    /**
     * 指定位置の動画のフレームを取得して、[Bitmap]で返す
     *
     * @param seekToMs シーク位置
     * @return Bitmap
     */
    suspend fun getVideoFrameBitmap(
        seekToMs: Long
    ): Bitmap = withContext(Dispatchers.Default) {
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
                awaitSeekToNextDecode(seekToMs)
                getImageReaderBitmap()
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
     */
    private suspend fun awaitSeekToNextDecode(
        seekToMs: Long
    ) = withContext(Dispatchers.Default) {
        val decodeMediaCodec = decodeMediaCodec!!
        val mediaExtractor = mediaExtractor!!
        val inputSurface = inputSurface!!

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
                // デコード結果が来ているか
                val outputBufferIndex = decodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // もう無い時
                        isDecoderOutputAvailable = false
                    }

                    outputBufferIndex >= 0 -> {
                        // ImageReader ( Surface ) に描画する
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)
                        // OpenGL で描画して、ImageReader で撮影する
                        // OpenGL 描画用スレッドに切り替えてから、swapBuffers とかやる
                        withContext(openGlRendererThreadDispatcher) {
                            if (doRender) {
                                var errorWait = false
                                try {
                                    inputSurface.awaitNewImage()
                                } catch (e: Exception) {
                                    errorWait = true
                                }
                                if (!errorWait) {
                                    inputSurface.drawImage()
                                    inputSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000)
                                    inputSurface.swapBuffers()
                                }
                            }
                        }
                        // 欲しいフレームの時間に到達した場合、ループを抜ける
                        val presentationTimeMs = bufferInfo.presentationTimeUs / 1000
                        if (seekToMs <= presentationTimeMs) {
                            isRunning = false
                            latestDecodePositionMs = presentationTimeMs
                        }
                    }
                }
            }

            // 次に進める
            mediaExtractor.advance()

            // 欲しいフレームが前回の呼び出しと連続していないときの処理
            // 例えば、前回の取得位置よりもさらに数秒以上先にシークした場合、指定位置になるまで待ってたら遅くなるので、数秒先にあるキーフレームまでシークする
            // で、このシークが必要かどうかの判定がこれ。数秒先をリクエストした結果、欲しいフレームが来るよりも先にキーフレームが来てしまった
            // この場合は一気にシーク位置に一番近いキーフレームまで進める
            // ただし、キーフレームが来ているサンプルの時間を比べて、欲しいフレームの位置の方が大きくなっていることを確認してから。
            // デコーダーの時間 presentationTimeUs と、MediaExtractor の sampleTime は同じじゃない？らしく、sampleTime の方がデコーダーの時間より早くなるので注意
            val isKeyFrame = mediaExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
            val currentSampleTimeMs = mediaExtractor.sampleTime / 1000
//            println("loop bufferInfo.presentationTimeUs = ${bufferInfo.presentationTimeUs / 1000} / sampleTime = ${mediaExtractor.sampleTime / 1000} / isKeyFrame = ${isKeyFrame} / seekToMs = $seekToMs")
            if (isKeyFrame && currentSampleTimeMs < seekToMs) {
//                println("mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)")
                mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
//                println("seekTo sampleTime = ${mediaExtractor.sampleTime / 1000}")
                decodeMediaCodec.flush()
            }
        }
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
                        // ImageReader ( Surface ) に描画する
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)
                        // OpenGL で描画して、ImageReader で撮影する
                        // OpenGL 描画用スレッドに切り替えてから、swapBuffers とかやる
                        withContext(openGlRendererThreadDispatcher) {
                            if (doRender) {
                                var errorWait = false
                                try {
                                    inputSurface.awaitNewImage()
                                } catch (e: Exception) {
                                    errorWait = true
                                }
                                if (!errorWait) {
                                    inputSurface.drawImage()
                                    inputSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000)
                                    inputSurface.swapBuffers()
                                }
                            }
                        }
                        // 欲しいフレームの時間に到達した場合、ループを抜ける
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

    /** [imageReader]から[Bitmap]を取り出す */
    private suspend fun getImageReaderBitmap(): Bitmap = withContext(Dispatchers.Default) {
        val image = imageReader!!.acquireLatestImage()
        val width = image.width
        val height = image.height
        val buffer = image.planes.first().buffer
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        prevBitmap = bitmap
        // Image を close する
        image.close()
        return@withContext bitmap
    }

    companion object {
        /** MediaCodec タイムアウト */
        private const val TIMEOUT_US = 10_000L
    }

}