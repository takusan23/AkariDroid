package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.github.takusan23.akaridroid.RenderData

/** 図形を描画する */
class ShapeRender(
    private val shape: RenderData.CanvasItem.Shape
) : BaseItemRender(), DrawCanvas {

    /** Canvas に描画する際に使う Paint */
    private val paint = Paint()

    override val layerIndex: Int
        get() = shape.layerIndex

    override suspend fun prepare() {
        // do noting
    }

    override suspend fun draw(canvas: Canvas, drawFrame: Bitmap, durationMs: Long, currentPositionMs: Long) {
        val (x, y) = shape.position
        val (width, height) = shape.size

        paint.color = Color.parseColor(shape.color)
        // 分ける
        when (shape.shapeType) {
            RenderData.CanvasItem.Shape.ShapeType.Rect -> {
                canvas.drawRect(x, y, (x + width), (y + height), paint)
            }

            RenderData.CanvasItem.Shape.ShapeType.Circle -> {
                // 半径
                val radius = width / 2f
                canvas.drawCircle(x + radius, y + radius, radius, paint)
            }
        }
    }

    override suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) {
        val (x, y) = shape.position
        val (width, height) = shape.size

        paint.color = Color.parseColor(shape.color)
        // 分ける
        when (shape.shapeType) {
            RenderData.CanvasItem.Shape.ShapeType.Rect -> {
                canvas.drawRect(x, y, (x + width), (y + height), paint)
            }

            RenderData.CanvasItem.Shape.ShapeType.Circle -> {
                // 半径
                val radius = width / 2f
                canvas.drawCircle(x + radius, y + radius, radius, paint)
            }
        }
    }

    override fun destroy() {
        // do nothing
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return shape == renderItem
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in shape.displayTime
    }
}