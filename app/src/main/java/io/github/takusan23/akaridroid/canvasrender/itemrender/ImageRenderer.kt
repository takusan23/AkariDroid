package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.VideoTrackRendererPrepareData
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.DrawCanvasInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.TimelineLifecycleRenderer
import io.github.takusan23.akaridroid.tool.FileTool

/** 写真を描画する */
class ImageRenderer(
    private val fileTool: FileTool,
    private val image: RenderData.CanvasItem.Image
) : TimelineLifecycleRenderer(), DrawCanvasInterface {

    override val layerIndex: Int
        get() = image.layerIndex

    /** Glide でロードした画像 */
    private var bitmap: Bitmap? = null

    /** Canvas に描画する際に使う Paint */
    private val paint = Paint()

    override suspend fun isReuse(renderItem: RenderData.CanvasItem, videoTrackRendererPrepareData: VideoTrackRendererPrepareData): Boolean {
        return image == renderItem
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in image.displayTime
    }

    override suspend fun enterTimeline() {
        super.enterTimeline()
        // リサイズする
        val (width, height) = image.size
        bitmap = fileTool.getBitmap(image.filePath, width, height)
    }

    override suspend fun leaveTimeline() {
        super.leaveTimeline()
        bitmap = null
    }

    override suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) {
        val bitmap = bitmap ?: return
        val (x, y) = image.position
        canvas.drawBitmap(bitmap, x, y, paint)
    }
}