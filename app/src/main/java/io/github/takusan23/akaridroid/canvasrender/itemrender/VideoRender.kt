package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.scale
import androidx.core.net.toUri
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.video.VideoFrameBitmapExtractor
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 動画を描画する */
class VideoRender(
    private val context: Context,
    private val video: RenderData.CanvasItem.Video
) : ItemRenderInterface {

    /** Bitmap を取り出す */
    private var videoFrameBitmapExtractor: VideoFrameBitmapExtractor? = null

    /** [preDraw]したときに取得する Bitmap */
    private var preLoadBitmap: Bitmap? = null

    private val paint = Paint()

    override val layerIndex: Int
        get() = video.layerIndex

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        videoFrameBitmapExtractor = VideoFrameBitmapExtractor().apply {
            // Uri と File で分岐
            when (video.filePath) {
                is RenderData.FilePath.File -> prepareDecoder(File(video.filePath.filePath).toAkariCoreInputOutputData())
                is RenderData.FilePath.Uri -> prepareDecoder(video.filePath.uriPath.toUri().toAkariCoreInputOutputData(context))
            }
        }
    }

    override suspend fun preDraw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        super.preDraw(canvas, durationMs, currentPositionMs)
        // 動画のフレーム取得は時間がかかるので、preDraw で取得する
        val videoFrameBitmapExtractor = videoFrameBitmapExtractor ?: return@withContext

        // カットする場合は考慮した時間を
        val framePositionFromCurrentPositionMs = currentPositionMs - video.displayTime.startMs
        val includeOffsetFramePositionMs = framePositionFromCurrentPositionMs + video.displayOffset.offsetFirstMs
        preLoadBitmap = videoFrameBitmapExtractor.getVideoFrameBitmap(includeOffsetFramePositionMs)?.let { origin ->
            // リサイズする場合
            val (width, height) = video.size
            origin.scale(width, height)
        }
    }

    override suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        val preLoadBitmap = preLoadBitmap ?: return@withContext
        val (x, y) = video.position
        canvas.drawBitmap(preLoadBitmap, x, y, paint)
    }

    override fun destroy() {
        videoFrameBitmapExtractor?.destroy()
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return renderItem == video
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean = currentPositionMs in video.displayTime
}