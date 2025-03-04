package io.github.takusan23.akaricore.video

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.MediaFormat
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.common.MediaExtractorTool
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorDynamicRangeMode
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorRenderingPrepareData
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoDecoder

/**
 * [android.media.MediaMetadataRetriever.getFrameAtTime]が遅いので、[MediaCodec]あたりを使って高速に[Bitmap]を返すやつを作る。
 * また、[android.media.MediaMetadataRetriever]を複数用意して、[Bitmap]を作ろうとしても何故か速度が変わらない（共有している・・？）
 *
 * TODO MediaParserKeyFrameTimeDetector を再度採用すべきかは検討。video パッケージは現状使ってないので優先度低。
 * https://github.com/takusan23/AkariDroid/blob/b3a2eaba323935f58aa88e847463520e86f53f6e/akari-core/src/main/java/io/github/takusan23/akaricore/video/MediaParserKeyFrameTimeDetector.kt
 */
class VideoFrameBitmapExtractor {

    /** 映像デコーダーから Bitmap として取り出すための ImageReader */
    private var imageReader: ImageReader? = null

    /** OpenGL ES で描画するやつ */
    private var akariGraphicsProcessor: AkariGraphicsProcessor? = null

    /** 動画デコーダー */
    private var akariVideoDecoder: AkariVideoDecoder? = null

    /** デコーダーの描画先 */
    private var akariGraphicsSurfaceTexture: AkariGraphicsSurfaceTexture? = null

    /** 前回[getImageReaderBitmap]で作成した Bitmap */
    private var prevBitmap: Bitmap? = null

    // 動画の縦横
    // どうやっても ImageReader でぶっ壊れた映像が出てくることがあるので（videoWidth = 1104 / videoHeight = 2560）、
    // 縦横同じサイズで ImageReader を作り、出てきた Bitmap を scale して戻す。
    private var videoHeight: Int = 0
    private var videoWidth: Int = 0

    // クロマキー
    private var chromakeyColor: Int? = null
    private var chromakeyThreshold: Float? = null

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
        this@VideoFrameBitmapExtractor.chromakeyColor = chromakeyColor
        this@VideoFrameBitmapExtractor.chromakeyThreshold = chromakeyThreshold

        val (mediaExtractor, index, mediaFormat) = MediaExtractorTool.extractMedia(input, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)!!
        mediaExtractor.selectTrack(index)

        videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
        videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)

        // もう使わないので
        mediaExtractor.release()

        // 16 で割り切れる数字にする
        // Snapdragon も Google Tensor も 16 の倍数じゃないと動画のフレームが乱れてしまう
        // TODO nearestImageReaderAvailableSize が必要な場合の判定
        val fixWidth = videoWidth.toFixImageReaderSupportValue()
        val fixHeight = videoHeight.toFixImageReaderSupportValue()

        // OpenGL ES の描画結果を受け取る
        imageReader = ImageReader.newInstance(fixWidth, fixHeight, PixelFormat.RGBA_8888, 2)

        // OpenGL ES の用意
        // MediaCodec と ImageReader の間に OpenGL を経由させる
        // 経由させないと、Google Pixel 以外（Snapdragon 端末とか）で動かなかった
        akariGraphicsProcessor = AkariGraphicsProcessor(
            AkariGraphicsProcessorRenderingPrepareData.SurfaceRendering(
                surface = imageReader!!.surface,
                width = videoWidth,
                height = videoHeight
            ),
            AkariGraphicsProcessorDynamicRangeMode.SDR
        ).apply { prepare() }

        // 映像デコーダー起動
        akariGraphicsSurfaceTexture = akariGraphicsProcessor!!.genTextureId { texId ->
            AkariGraphicsSurfaceTexture(texId)
        }
        akariVideoDecoder = AkariVideoDecoder().apply {
            prepare(
                input = input,
                outputSurface = akariGraphicsSurfaceTexture!!.surface
            )
        }
    }

    /** デコーダーを破棄する */
    suspend fun destroy() {
        imageReader?.close()
        akariVideoDecoder?.destroy()
        akariGraphicsSurfaceTexture?.destroy()
        akariGraphicsProcessor?.destroy()
    }

    /**
     * 指定位置の動画のフレームを取得して、[Bitmap]で返す
     *
     * @param seekToMs シーク位置
     * @return Bitmap。もうデータがない場合は null。
     */
    suspend fun getVideoFrameBitmap(
        seekToMs: Long
    ): Bitmap? {
        val akariVideoDecoder = akariVideoDecoder!!
        val akariGraphicsProcessor = akariGraphicsProcessor!!
        val akariGraphicsSurfaceTexture = akariGraphicsSurfaceTexture!!

        val isFrameSuccessful = akariVideoDecoder.seekTo(seekToMs = seekToMs)
        if (!isFrameSuccessful) return null

        akariGraphicsProcessor.drawOneshot {
            drawSurfaceTexture(
                akariSurfaceTexture = akariGraphicsSurfaceTexture,
                chromakeyThreshold = chromakeyThreshold,
                chromaKeyColor = chromakeyColor
            )
        }
        return getImageReaderBitmap()
    }

    /** [imageReader]から[Bitmap]を取り出す */
    private suspend fun getImageReaderBitmap(): Bitmap {
        // ImageReader から取り出して、アスペクト比を戻す
        val fixAspectRateBitmap = imageReader!!.getImageReaderBitmap(videoWidth, videoHeight)
        prevBitmap = fixAspectRateBitmap
        return fixAspectRateBitmap
    }

}