package io.github.takusan23.akaridroid.canvasrender.itemrender.v2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.TextRender.Companion.createPaint
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.DrawCanvasInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.TimelineLifecycleRenderer
import io.github.takusan23.akaridroid.tool.FontManager

/** 文字を描画する */
class TextRenderer(
    private val context: Context,
    private val text: RenderData.CanvasItem.Text
) : TimelineLifecycleRenderer(), DrawCanvasInterface {

    override val layerIndex: Int
        get() = text.layerIndex

    // 枠なし文字
    private val fillPaint = createPaint(text).apply {
        style = Paint.Style.FILL
    }

    // 枠あり文字の際に枠を書く
    private val strokePaint = createPaint(text).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return text == renderItem
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in text.displayTime
    }

    override suspend fun enterTimeline() {
        super.enterTimeline()
        // フォントをロードする
        val fontManager = FontManager(context)
        text.fontName
            ?.let { fontName -> fontManager.createTypeface(fontName) }
            ?.also { typeface ->
                fillPaint.typeface = typeface
                strokePaint.typeface = typeface
            }
    }

    override suspend fun leaveTimeline() {
        super.leaveTimeline()
        // do nothing
    }

    override suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) {
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
}