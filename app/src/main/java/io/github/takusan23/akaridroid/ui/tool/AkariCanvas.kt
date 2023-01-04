package io.github.takusan23.akaridroid.ui.tool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
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