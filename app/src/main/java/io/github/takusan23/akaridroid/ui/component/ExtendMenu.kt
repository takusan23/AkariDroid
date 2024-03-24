package io.github.takusan23.akaridroid.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R

/**
 * 押せない、常に広げておく[ExtendMenu]
 *
 * @param modifier [Modifier]
 * @param label メニューの名前
 * @param iconResId アイコン
 * @param description メニューの名前の下に表示する文字
 * @param currentMenu 選択中の文言
 * @param menuContent [MenuItem]を置くための
 */
@Composable
fun NoOpenableExtendMenu(
    modifier: Modifier = Modifier,
    label: String,
    iconResId: Int,
    currentMenu: String?,
    description: String? = null,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(10.dp)
    ) {
        InternalExtendMenu(
            label = label,
            iconResId = iconResId,
            currentMenu = currentMenu,
            description = description,
            menuContent = menuContent
        )
    }
}

/**
 * 押したら広がるメニュー
 *
 * @param modifier [Modifier]
 * @param isOpen 展開するか
 * @param label メニューの名前
 * @param iconResId アイコン
 * @param description メニューの名前の下に表示する文字
 * @param currentMenu 選択中の文言
 * @param onOpenChange 押した時
 * @param menuContent [MenuItem]を置くための
 */
@Composable
fun ExtendMenu(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    label: String,
    iconResId: Int,
    currentMenu: String?,
    description: String? = null,
    onOpenChange: () -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onOpenChange,
        shape = RoundedCornerShape(10.dp)
    ) {
        InternalExtendMenu(
            label = label,
            iconResId = iconResId,
            currentMenu = currentMenu,
            description = description,
            icon = {
                Icon(
                    imageVector = if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        ) {
            AnimatedVisibility(visible = isOpen) {
                Column {
                    menuContent(this)
                }
            }
        }
    }
}

/**
 * 各メニュー
 *
 * @param modifier [Modifier]
 * @param title タイトル
 * @param description 説明
 * @param isSelect 選択中なら。チェックマークがつきます
 * @param onClick 押した時
 */
@Composable
fun ExtendMenuItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
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
                    text = title,
                    fontSize = 18.sp
                )
                Text(text = description)
            }

            if (isSelect) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_done_24), contentDescription = null)
            }
        }
    }
}

/** [ExtendMenu]、[NoOpenableExtendMenu]の共通部分  */
@Composable
private fun InternalExtendMenu(
    modifier: Modifier = Modifier,
    label: String,
    iconResId: Int,
    currentMenu: String?,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
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
                if (description != null) {
                    Text(
                        text = description,
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                }
            }

            if (currentMenu != null) {
                Text(text = currentMenu)
            }

            if (icon != null) {
                icon()
            }
        }

        menuContent(this)
    }
}
