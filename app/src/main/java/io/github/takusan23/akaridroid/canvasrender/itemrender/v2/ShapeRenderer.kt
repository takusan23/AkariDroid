package io.github.takusan23.akaridroid.canvasrender.itemrender.v2

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.DrawCanvasInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.TimelineLifecycleRenderer

/** 図形を描画する */
class ShapeRenderer(
    private val shape: RenderData.CanvasItem.Shape
) : TimelineLifecycleRenderer(), DrawCanvasInterface {

    override val layerIndex: Int
        get() = shape.layerIndex

    /** Canvas に描画する際に使う Paint */
    private val paint = Paint().apply {
        color = Color.parseColor(shape.color)
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return shape == renderItem
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in shape.displayTime
    }

    override suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) {
        val (x, y) = shape.position
        val (width, height) = shape.size

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
}