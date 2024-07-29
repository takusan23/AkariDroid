package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.tool.FontManager

/** 文字を描画する */
class TextRender(
    private val context: Context,
    private val text: RenderData.CanvasItem.Text
) : BaseItemRender() {

    // 枠なし文字
    private val fillPaint = createPaint(text).apply {
        style = Paint.Style.FILL
    }

    // 枠あり文字の際に枠を書く
    private val strokePaint = createPaint(text).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override val layerIndex: Int
        get() = text.layerIndex

    override suspend fun prepare() {
        // フォントをロードする
        val fontManager = FontManager(context)
        text.fontName
            ?.let { fontName -> fontManager.createTypeface(fontName) }
            ?.also { typeface ->
                fillPaint.typeface = typeface
                strokePaint.typeface = typeface
            }
    }

    override suspend fun draw(canvas: Canvas, drawFrame: Bitmap, durationMs: Long, currentPositionMs: Long) {
        fillPaint.color = Color.parseColor(text.fontColor)
        fillPaint.textSize = text.textSize

        // 枠取りにするなら
        val isDrawStroke = text.strokeColor != null
        if (isDrawStroke) {
            strokePaint.color = Color.parseColor(text.strokeColor)
            strokePaint.textSize = text.textSize
        }

        val (x, y) = text.position

        // 複数行サポート
        text.text.lines().forEachIndexed { index, text ->
            canvas.drawText(text, x, y + (fillPaint.textSize * index), fillPaint)

            if (isDrawStroke) {
                canvas.drawText(text, x, y + (fillPaint.textSize * index), strokePaint)
            }
        }
    }

    override suspend fun draw(textureRenderer: AkariGraphicsTextureRenderer, durationMs: Long, currentPositionMs: Long) {
        fillPaint.color = Color.parseColor(text.fontColor)
        fillPaint.textSize = text.textSize

        // 枠取りにするなら
        val isDrawStroke = text.strokeColor != null
        if (isDrawStroke) {
            strokePaint.color = Color.parseColor(text.strokeColor)
            strokePaint.textSize = text.textSize
        }

        val (x, y) = text.position

        textureRenderer.drawCanvas {
            // 複数行サポート
            text.text.lines().forEachIndexed { index, text ->
                drawText(text, x, y + (fillPaint.textSize * index), fillPaint)

                if (isDrawStroke) {
                    drawText(text, x, y + (fillPaint.textSize * index), strokePaint)
                }
            }
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
         * フォントの読み込みまではしないので、フォントを変更している場合はサイズが変わってしまう。
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