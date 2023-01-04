package io.github.takusan23.akaridroid.ui.component

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.data.CanvasElementType

/**
 * キャンバス
 * 仮です。動画の幅に合っていないため、これは要素が描画されるかどうか のみ 見ています。
 *
 * @param modifier [Modifier]
 * @param elementList 描画する要素
 */
@Composable
fun AkariCanvas(
    modifier: Modifier = Modifier,
    elementList: List<CanvasElementData>
) {

    val paint = remember { Paint() }

    Canvas(modifier = modifier) {
        drawIntoCanvas {
            elementList.forEach { element ->
                when (val elementType = element.elementType) {
                    is CanvasElementType.TextElement -> {
                        it.nativeCanvas.drawText(
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