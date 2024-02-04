package io.github.takusan23.akaridroid.v2.canvasrender.itemrender

import android.graphics.Canvas
import android.graphics.Paint
import io.github.takusan23.akaridroid.v2.canvasrender.RenderData

/** 文字を描画する */
class TextRender(
    private val text: RenderData.CanvasItem.Text
) : ItemRenderInterface {

    /** 文字の大きさとか */
    private val paint = Paint()

    override val displayTime: RenderData.DisplayTime
        get() = text.displayTime

    override suspend fun prepare() {
        // do nothing
    }

    override suspend fun draw(canvas: Canvas, currentPositionMs: Long) {
        if (currentPositionMs !in text.displayTime) {
            return
        }
        if (text.fontColor != null) {
            paint.color = text.fontColor
        }
        if (text.textSize != null) {
            paint.textSize = text.textSize
        }
        val (x, y) = text.position
        canvas.drawText(text.text, x, y, paint)
    }

    override suspend fun destroy() {
        // do nothing
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return text != renderItem
    }
}