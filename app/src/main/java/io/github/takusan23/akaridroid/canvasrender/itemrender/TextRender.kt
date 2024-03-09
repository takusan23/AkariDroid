package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.github.takusan23.akaridroid.RenderData

/** 文字を描画する */
class TextRender(
    private val text: RenderData.CanvasItem.Text
) : ItemRenderInterface {

    /** 文字の大きさとか */
    private val paint = createPaint(text)

    override val layerIndex: Int
        get() = text.layerIndex

    override suspend fun prepare() {
        // do nothing
    }

    override suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) {
        paint.color = Color.parseColor(text.fontColor)
        paint.textSize = text.textSize
        val (x, y) = text.position
        canvas.drawText(text.text, x, y, paint)
    }

    override fun destroy() {
        // do nothing
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return text == renderItem
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in text.displayTime
    }

    companion object {

        /**
         * [TextRender]で利用されている[Paint]を返す。
         * 文字を書くときに必要な縦横サイズの測定に。
         *
         * @param text 文字サイズとか。[RenderData.CanvasItem.Text]
         * @return [Paint]
         */
        fun createPaint(text: RenderData.CanvasItem.Text): Paint = Paint().apply {
            color = Color.parseColor(text.fontColor)
            textSize = text.textSize
        }

    }
}