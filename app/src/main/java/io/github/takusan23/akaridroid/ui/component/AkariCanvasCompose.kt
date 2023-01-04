package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.ui.tool.AkariCanvas

/**
 * キャンバス
 * Bitmapにしたのを表示しているので、多分エンコード結果と同じになるはずです。
 *
 * @param modifier [Modifier]
 * @param elementList 描画する要素
 */
@Composable
fun AkariCanvasCompose(
    modifier: Modifier = Modifier,
    videoWidth: Int = 1280,
    videoHeight: Int = 720,
    elementList: List<CanvasElementData>,
) {

    val (bitmap, canvas) = remember { AkariCanvas.createCanvas(videoWidth, videoHeight) }

    LaunchedEffect(key1 = elementList) {
        AkariCanvas.render(canvas, elementList)
    }

    Image(
        modifier = modifier,
        painter = BitmapPainter(bitmap.asImageBitmap()),
        contentDescription = null
    )

}