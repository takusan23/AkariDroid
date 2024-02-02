package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.ui.tool.AkariCanvas

/**
 * キャンバス
 * Bitmapにしたのを表示しているので、多分エンコード結果と同じになるはずです。
 *
 * @param modifier [Modifier]
 * @param elementList 描画する要素
 * @param videoHeight 動画の高さ
 * @param videoWidth 動画の幅
 */
@Composable
fun AkariCanvasCompose(
    modifier: Modifier = Modifier,
    videoWidth: Int,
    videoHeight: Int,
    elementList: List<CanvasElementData>,
) {

    val (bitmap, canvas) = remember { AkariCanvas.createCanvas(videoWidth, videoHeight) }
    val hittingElement = remember { mutableStateOf<CanvasElementData?>(null) }

    LaunchedEffect(key1 = elementList) {
        AkariCanvas.render(canvas, elementList)
    }

    Box {
        Image(
            modifier = modifier
                // ElementList 更新時に再起動するため
                .pointerInput(key1 = elementList) {
                    detectDragGestures(
                        onDragStart = { (x, y) ->
                            val hitBox = AkariCanvas.calcElementHitBox(elementList)
                            hittingElement.value = hitBox.firstOrNull { (_, rectF) -> rectF.contains(x, y) }?.first
                        },
                        onDragEnd = {
                            hittingElement.value = null
                        },
                        onDrag = { change, (x, y) ->
                            change.consume()
                        }
                    )
                },
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = null
        )
        if (hittingElement.value != null) {
            Text(text = "あたり = ${hittingElement.value}")
        }
    }

}