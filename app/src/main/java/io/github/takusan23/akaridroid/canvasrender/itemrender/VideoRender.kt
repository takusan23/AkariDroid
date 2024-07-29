package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.Matrix
import androidx.core.net.toUri
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer
import io.github.takusan23.akaricore.graphics.AkariGraphicsVideoTexture
import io.github.takusan23.akaricore.video.VideoFrameBitmapExtractor
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * とりあえずここに置かせて；；
 * 再生位置を渡して、素材の再生位置を出す。再生速度、オフセットが考慮される。
 *
 * @param currentPositionMs 動画全体の再生位置
 * @return 動画素材の取り出すフレームの時間
 */
fun RenderData.CanvasItem.Video.calcVideoFramePositionMs(currentPositionMs: Long): Long {
    // 素材の開始位置から見た positionMs にする
    val currentPositionMsInItem = currentPositionMs - displayTime.startMs
    // 再生速度を考慮した positionMsInItem にする
    val currentPositionMsInPlaybackSpeed = (currentPositionMsInItem * displayTime.playbackSpeed).toLong()
    // オフセット、読み飛ばす分を考慮
    // offset は再生速度考慮している
    return currentPositionMsInPlaybackSpeed + displayOffset.offsetFirstMs
}

/** 動画を描画する */
class VideoRender(
    initTexId: Int,
    private val context: Context,
    private val video: RenderData.CanvasItem.Video
) : BaseItemRender() {

    /** 動画をデコードして、フレームを[AkariGraphicsTextureRenderer]へ描画するやつ */
    private val akariGraphicsVideoTexture = AkariGraphicsVideoTexture(initTexId)

    /** Bitmap を取り出す */
    private var videoFrameBitmapExtractor: VideoFrameBitmapExtractor? = null

    /** [preDraw]したときに取得する Bitmap */
    private var preLoadBitmap: Bitmap? = null

    private val paint = Paint()

    override val layerIndex: Int
        get() = video.layerIndex

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        // クロマキーする場合
        val isEnableChromaKey = video.chromaKeyColor != null
        videoFrameBitmapExtractor = VideoFrameBitmapExtractor().apply {
            // Uri と File で分岐
            prepareDecoder(
                input = when (video.filePath) {
                    is RenderData.FilePath.File -> File(video.filePath.filePath).toAkariCoreInputOutputData()
                    is RenderData.FilePath.Uri -> video.filePath.uriPath.toUri().toAkariCoreInputOutputData(context)
                },
                chromakeyThreshold = if (isEnableChromaKey) CHROMAKEY_THRESHOLD else null,
                chromakeyColor = if (isEnableChromaKey) video.chromaKeyColor!! else null
            )
        }

        akariGraphicsVideoTexture.prepareDecoder(
            input = when (video.filePath) {
                is RenderData.FilePath.File -> File(video.filePath.filePath).toAkariCoreInputOutputData()
                is RenderData.FilePath.Uri -> video.filePath.uriPath.toUri().toAkariCoreInputOutputData(context)
            },
            chromakeyThreshold = if (isEnableChromaKey) CHROMAKEY_THRESHOLD else null,
            chromakeyColor = if (isEnableChromaKey) video.chromaKeyColor!! else null
        )
    }

    override suspend fun preDraw(durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        super.preDraw(durationMs, currentPositionMs)

/*
        // 動画のフレーム取得は時間がかかるので、preDraw で取得する
        val videoFrameBitmapExtractor = videoFrameBitmapExtractor ?: return@withContext

        // 再生速度、オフセットを考慮した、動画のフレーム取得時間を出す
        val framePositionMs = video.calcVideoFramePositionMs(currentPositionMs = currentPositionMs)

        // 取り出す
        preLoadBitmap = videoFrameBitmapExtractor.getVideoFrameBitmap(seekToMs = framePositionMs)?.let { origin ->
            // リサイズする場合
            val (width, height) = video.size
            origin.scale(width, height)
        }
*/
    }

    override suspend fun draw(canvas: Canvas, drawFrame: Bitmap, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        val preLoadBitmap = preLoadBitmap ?: return@withContext
        val (x, y) = video.position
        canvas.drawBitmap(preLoadBitmap, x, y, paint)
    }

    override suspend fun draw(textureRenderer: AkariGraphicsTextureRenderer, durationMs: Long, currentPositionMs: Long) {
        akariGraphicsVideoTexture.draw(
            akariGraphicsTextureRenderer = textureRenderer,
            seekToMs = currentPositionMs,
            onTransform = { mvpMatrix ->
                // リサイズする場合
                val (width, height) = video.size
                Matrix.scaleM(mvpMatrix, 0, width / akariGraphicsVideoTexture.videoWidth.toFloat(), height / akariGraphicsVideoTexture.videoHeight.toFloat(), 1f)
                // 位置調整
                val (x, y) = video.position
                Matrix.translateM(mvpMatrix, 0, x / textureRenderer.width.toFloat(), y / textureRenderer.height.toFloat(), 1f)
            }
        )
    }

    override fun destroy() {
        videoFrameBitmapExtractor?.destroy()
        akariGraphicsVideoTexture.destroy()
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return renderItem == video
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean = currentPositionMs in video.displayTime

    companion object {
        private const val CHROMAKEY_THRESHOLD = 0.3f // TODO ユーザー入力で変更できるようにする
    }
}