package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.data.ExtendDropDownMenuItem

/**
 * 広いドロップダウンメニュー。
 * Android の通知ドロワーで複数の通知を広げたときのあれに近い。
 *
 * @param modifier [Modifier]
 * @param isOpen メニューを開くか
 * @param label ラベル
 * @param iconResId ラベルの隣のアイコン
 * @param selectIndex 選択中の位置
 * @param menuList メニュー一覧
 * @param onOpenChange メニュー展開、格納時に呼ばれる
 * @param onSelect メニューを選択したら
 */
@Composable
fun ExtendDropDownMenu(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    label: String,
    iconResId: Int,
    selectIndex: Int,
    menuList: List<ExtendDropDownMenuItem>,
    onOpenChange: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val subtext = remember { menuList.joinToString { it.title } }
    // surfaceColorAtElevation で高さを付ける
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onOpenChange,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        fontSize = 20.sp
                    )
                    Text(
                        text = subtext,
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                }

                Text(text = menuList[selectIndex].title)

                Icon(
                    imageVector = if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (isOpen) {
                menuList.forEachIndexed { index, menuItem ->
                    MenuItem(
                        modifier = Modifier,
                        menuItem = menuItem,
                        isSelect = selectIndex == index,
                        onClick = {
                            onSelect(index)
                            onOpenChange()
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * 各メニュー
 *
 * @param modifier [Modifier]
 * @param menuItem メニュー
 * @param isSelect 選択中なら
 * @param onClick 押した時
 */
@Composable
private fun MenuItem(
    modifier: Modifier = Modifier,
    menuItem: ExtendDropDownMenuItem,
    isSelect: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = menuItem.title,
                    fontSize = 18.sp
                )
                Text(text = menuItem.description)
            }

            if (isSelect) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_done_24), contentDescription = null)
            }
        }
    }
}