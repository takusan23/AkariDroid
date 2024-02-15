package io.github.takusan23.akaridroid.v2.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.bumptech.glide.Glide
import io.github.takusan23.akaridroid.v2.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 写真を描画する */
class ImageRender(
    private val context: Context,
    private val image: RenderData.CanvasItem.Image
) : ItemRenderInterface {

    /** Glide でロードした画像 */
    private var bitmap: Bitmap? = null

    /** Canvas に描画する際に使う Paint */
    private val paint = Paint()

    override val displayTime: RenderData.DisplayTime
        get() = image.displayTime

    override suspend fun prepare() = withContext(Dispatchers.IO) {
        val request = Glide
            .with(context)
            .asBitmap()
            .load(image.filePath)

        // リサイズする
        bitmap = if (image.size != null) {
            val (width, height) = image.size
            request.submit(width, height).get()
        } else {
            request.submit().get()
        }
    }

    override suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        val bitmap = bitmap ?: return@withContext
        val (x, y) = image.position
        canvas.drawBitmap(bitmap, x, y, paint)
    }

    override suspend fun destroy() {
        bitmap?.recycle()
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return image != renderItem
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in image.displayTime
    }

}