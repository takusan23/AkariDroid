package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.bumptech.glide.Glide
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 写真を描画する */
class ImageRender(
    private val context: Context,
    private val image: RenderData.CanvasItem.Image
) : BaseItemRender() {

    /** Glide でロードした画像 */
    private var bitmap: Bitmap? = null

    /** Canvas に描画する際に使う Paint */
    private val paint = Paint()

    override val layerIndex: Int
        get() = image.layerIndex

    override suspend fun prepare() = withContext(Dispatchers.IO) {
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
        bitmap = request.submit(width, height).get()
    }

    override suspend fun draw(canvas: Canvas, drawFrame: Bitmap, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.IO) {
        val bitmap = bitmap ?: return@withContext
        val (x, y) = image.position
        canvas.drawBitmap(bitmap, x, y, paint)
    }

    override fun destroy() {
        bitmap = null
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return image == renderItem
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in image.displayTime
    }

}