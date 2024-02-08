package io.github.takusan23.akaricore.v2.video

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import io.github.takusan23.akaricore.v1.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * [android.media.MediaMetadataRetriever.getFrameAtTime]が遅いので、[MediaCodec]あたりを使って高速に[Bitmap]を返すやつを作る。
 * また、[android.media.MediaMetadataRetriever]を複数用意して、[Bitmap]を作ろうとしても何故か速度が変わらない（共有している・・？）
 */
class VideoFrameBitmapExtractor {

    /** MediaCodec デコーダー */
    private var decodeMediaCodec: MediaCodec? = null

    /** Extractor */
    private var mediaExtractor: MediaExtractor? = null

    /** 映像デコーダーから Bitmap として取り出すための ImageReader */
    private var imageReader: ImageReader? = null

    /** 最後の[getVideoFrameBitmap]で取得したフレームの位置 */
    private var latestDecodePositionMs = 0L

    /** 前回のシーク位置 */
    private var prevSeekToMs = -1L

    /** 前回[getImageReaderBitmap]で作成した Bitmap */
    private var prevBitmap: Bitmap? = null

    /**
     * デコーダーを初期化する
     *
     * @param videoFile 動画ファイル
     */
    suspend fun prepareDecoder(
        videoFile: File
    ) = withContext(Dispatchers.IO) {
        // コンテナからメタデータを取り出す
        val (mediaExtractor, index, mediaFormat) = MediaExtractorTool.extractMedia(videoFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)!!
        this@VideoFrameBitmapExtractor.mediaExtractor = mediaExtractor
        mediaExtractor.selectTrack(index)

        val codecName = mediaFormat.getString(MediaFormat.KEY_MIME)!!
        val videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)

        // Surface 経由で Bitmap が取れる ImageReader つくる
        imageReader = ImageReader.newInstance(videoWidth, videoHeight, ImageFormat.YUV_420_888, 2)

        // 映像デコーダー起動
        decodeMediaCodec = MediaCodec.createDecoderByType(codecName).apply {
            configure(mediaFormat, imageReader!!.surface, null, 0)
        }
        decodeMediaCodec!!.start()
    }

    /** デコーダーを破棄する */
    fun destroy() {
        decodeMediaCodec?.release()
        mediaExtractor?.release()
        imageReader?.close()
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

        var isRunning = isActive
        val bufferInfo = MediaCodec.BufferInfo()
        while (isRunning) {
            // コンテナフォーマットからサンプルを取り出し、デコーダーに渡す
            // シークしないことで、連続してフレームを取得する場合にキーフレームまで戻る必要がなくなり、早くなる
            val inputBufferIndex = decodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = decodeMediaCodec.getInputBuffer(inputBufferIndex)!!
                // デコーダーへ流す
                val size = mediaExtractor.readSampleData(inputBuffer, 0)
                decodeMediaCodec.queueInputBuffer(inputBufferIndex, 0, size, mediaExtractor.sampleTime, 0)
                mediaExtractor.advance()
                // 欲しいフレームが前回の呼び出しと連続していないときの処理
                // つまり、欲しいフレームが来るよりも先にキーフレームが来てしまった
                // この場合は一気にシーク市に一番近いキーフレームまで戻る
                if ((mediaExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                }
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
                        // ImageReader ( Surface ) に描画する
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)
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
        // ImageFormat.YUV_420_888 を NV21 へ
        val image = imageReader!!.acquireLatestImage()
        val width = image.width
        val height = image.height
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        // NV21 から JPEG へ
        val byteArrayOutputStream = ByteArrayOutputStream()
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, byteArrayOutputStream)
        // JPEG から Bitmap へ
        val jpegByteArray = byteArrayOutputStream.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.size)
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