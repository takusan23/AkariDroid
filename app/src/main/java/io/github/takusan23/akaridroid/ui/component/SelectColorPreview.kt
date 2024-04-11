package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.tool.ColorTool

/**
 * 色が何色か、RGB でも表示するコンポーネント。カラーピッカーとかで使っている
 *
 * @param modifier [Modifier]
 * @param currentColor 選択中の色
 */
@Composable
fun SelectColorPreview(
    modifier: Modifier = Modifier,
    currentColor: Color
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // RGB の配列
        val rgbValueList = ColorTool.toRgbList(currentColor)

        ColorItem(color = currentColor)

        Column {

            Row {
                listOf(
                    "R: ${rgbValueList[0]}",
                    "G: ${rgbValueList[1]}",
                    "B: ${rgbValueList[2]}"
                ).forEach { text ->
                    Text(
                        modifier = Modifier.weight(1f),
                        text = text
                    )
                }
            }

            Text(text = ColorTool.toHexColorCode(currentColor))
        }
    }
}

/**
 * 色の選択とプレビューで使ってる。あの四角の色のやつ。
 *
 * @param modifier [Modifier]
 * @param color 色
 * @param onClick 押した時
 */
@Composable
fun ColorItem(
    modifier: Modifier = Modifier,
    color: Color,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.size(50.dp),
        color = color,
        border = BorderStroke(width = 2.dp, color = LocalContentColor.current),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick,
        content = { /* do nothing */ }
    )
}