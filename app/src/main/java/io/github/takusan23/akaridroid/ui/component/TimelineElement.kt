package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * タイムラインの各要素
 *
 * @param modifier [Modifier]
 * @param color 色
 * @param text テキスト
 * @param onClick 押したら呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineElement(
    modifier: Modifier = Modifier,
    color: Color,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        color = color,
        contentColor = contentColorFor(color),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                modifier = Modifier.padding(start = 10.dp),
                text = text
            )
        }
    }
}