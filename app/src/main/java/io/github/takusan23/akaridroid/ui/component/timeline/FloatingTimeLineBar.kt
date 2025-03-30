package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * タイムラインの下にある浮いているバー
 *
 * @param modifier [Modifier]
 * @param menu [FloatingTimeLineItem]など
 */
@Composable
fun FloatingTimeLineBar(
    modifier: Modifier = Modifier,
    menu: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = CircleShape,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(
                vertical = 5.dp, // 上下にちょっと確保
                horizontal = 10.dp // 角が丸いので
            ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = menu
        )
    }
}

/**
 * タイトル付きメニュー
 *
 * @param modifier [Modifier]
 * @param title メニュー名
 * @param iconResId アイコンのリソースID
 * @param isEnable ボタンが有効か
 * @param onClick ボタンが押されたとき
 */
@Composable
fun FloatingTimeLineTitledItem(
    modifier: Modifier = Modifier,
    title: String,
    iconResId: Int,
    isEnable: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        modifier = modifier,
        enabled = isEnable,
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = title)
    }
}

/**
 * アイコンだけのメニュー
 *
 * @param modifier [Modifier]
 * @param iconResId アイコンのリソースID
 * @param isEnable ボタンが有効か
 * @param onClick ボタンが押されたとき
 */
@Composable
fun FloatingTimeLineItem(
    modifier: Modifier = Modifier,
    iconResId: Int,
    isEnable: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier,
        enabled = isEnable,
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null
        )
    }
}