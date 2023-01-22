package io.github.takusan23.akaridroid.ui.tool

import android.graphics.*
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.data.CanvasElementType

object AkariCanvas {

    /**
     * Canvasを作成する
     *
     * @param videoWidth 動画の幅
     * @param videoHeight 動画の高さ
     */
    fun createCanvas(videoWidth: Int, videoHeight: Int): Pair<Bitmap, Canvas> {
        val bitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        return bitmap to canvas
    }

    /**
     * それぞれの要素の位置を[Rect]で返す
     *
     * @param elementList Canvas要素
     * @return Canvas要素と座標
     */
    fun calcElementHitBox(elementList: List<CanvasElementData>): List<Pair<CanvasElementData, RectF>> {
        val paint = Paint()
        return elementList.map { elementData ->
            when (val elementType = elementData.elementType) {
                is CanvasElementType.TextElement -> {
                    paint.apply {
                        color = elementType.color
                        textSize = elementType.fontSize
                    }
                    val textWidth = paint.measureText(elementType.text)
                    val textSize = paint.textSize
                    elementData to RectF(elementData.xPos, elementData.yPos, elementData.xPos + textWidth, elementData.yPos + textSize)
                }
            }
        }
    }

    /**
     * Canvasに [CanvasElementData] を描画する
     *
     * @param canvas [createCanvas]参照
     * @param elementList 描画する要素
     */
    fun render(canvas: Canvas, elementList: List<CanvasElementData>) {
        val paint = Paint()

        canvas.apply {

            // 前回のを消す
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)

            elementList.forEach { element ->
                when (val elementType = element.elementType) {
                    is CanvasElementType.TextElement -> {
                        drawText(
                            elementType.text,
                            element.xPos,
                            element.yPos,
                            paint.apply {
                                color = elementType.color
                                textSize = elementType.fontSize
                            }
                        )
                    }
                }
            }

        }
    }

}