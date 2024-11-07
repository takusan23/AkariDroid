package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.bumptech.glide.Glide
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.DrawCanvasInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.TimelineLifecycleRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 写真を描画する */
class ImageRenderer(
    private val context: Context,
    private val image: RenderData.CanvasItem.Image
) : TimelineLifecycleRenderer(), DrawCanvasInterface {

    override val layerIndex: Int
        get() = image.layerIndex

    /** Glide でロードした画像 */
    private var bitmap: Bitmap? = null

    /** Canvas に描画する際に使う Paint */
    private val paint = Paint()

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return image == renderItem
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in image.displayTime
    }

    override suspend fun enterTimeline() {
        super.enterTimeline()
        val request = Glide
            .with(context)
            .asBitmap()
            .load(
                when (image.filePath) {
                    is RenderData.FilePath.File -> image.filePath.filePath
                    is RenderData.FilePath.Uri -> image.filePath.uriPath
                }
            )

        // リサイズする
        val (width, height) = image.size
        bitmap = withContext(Dispatchers.IO) {
            request.submit(width, height).get()
        }
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