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

        // 複数行サポート
        text.text.lines().forEachIndexed { index, text ->
            canvas.drawText(text, x, y + (paint.textSize * index), paint)
        }
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

        /**
         * 文字を描画した時のサイズを出す。
         * [TextRender]には[RenderData.Size]が生えていないので代わり。
         * [Paint.measureText]だと、複数行の文字に対応できないため。
         *
         * @param text [RenderData.CanvasItem.Text]
         * @return 計測結果
         */
        fun analyzeDrawSize(text: RenderData.CanvasItem.Text): RenderData.Size {
            val paint = createPaint(text)
            val textLineList = text.text.lines()
            // 幅は改行全部見て、最大値
            val textWidth = textLineList.maxOf { lineText -> paint.measureText(lineText) }
            // 高さは文字サイズと改行回数をかける
            val textHeight = paint.textSize * textLineList.size
            return RenderData.Size(width = textWidth.toInt(), height = textHeight.toInt())
        }

        /**
         * 与えられた高さから、[text]を1行描画するのに必要な文字サイスを出す
         *
         * @param text [RenderData.CanvasItem.Text]
         * @param height 高さ
         * @return 文字サイス
         */
        fun analyzeTextSize(text: RenderData.CanvasItem.Text, height: Int): Float {
            val textLineList = text.text.lines()
            return (height / textLineList.size).toFloat()
        }

    }
}