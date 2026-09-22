package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * [io.github.takusan23.akaridroid.ui.sheet.MenuBottomSheet]の各メニュー
 *
 * @param modifier [Modifier]
 * @param title 名前
 * @param description 説明
 * @param iconResId アイコンのリソース
 * @param onClick 押した時
 */
@Composable
fun BottomSheetMenuItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    iconResId: Int,
    shape: Shape,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 20.sp)
                Text(text = description)
            }
        }
    }
}

/**
 * シートで使うスイッチ付きのメニュー
 *
 * @param modifier [Modifier]
 * @param title 名前
 * @param description 説明
 * @param iconResId アイコンのリソース
 * @param shape 角丸の形状
 * @param checked スイッチON/OFF
 * @param onCheckedChange スイッチの状態が変わったとき
 */
@Composable
fun BottomSheetMenuSwitchItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    iconResId: Int,
    shape: Shape,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val latestChecked = remember { mutableStateOf(checked) }

    Surface(
        modifier = modifier,
        checked = latestChecked.value,
        onCheckedChange = {
            onCheckedChange(it)
            latestChecked.value = it
        },
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 20.sp)
                Text(text = description)
            }
            Switch(
                checked = latestChecked.value,
                onCheckedChange = null // toggleable でやっているので
            )
        }
    }
}