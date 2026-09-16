package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.takusan23.akaridroid.R

/** メニュー選択結果 */
enum class LargeScreenMenuSwitchMode {
    /** メニュー。エンコードとかの */
    Menu,

    /** 追加。動画や文字 */
    AddRenderItem
}

/**
 * 大画面の時に隣に出るメニューの切り替えボタン
 *
 * @param modifier [Modifier]
 * @param current 今選択してる値
 * @param onMenuSelect メニューを押したとき
 * @param onAddRenderItem 追加を押したとき
 */
@Composable
fun LargeScreenMenuSwitchButtonGroup(
    modifier: Modifier = Modifier,
    current: LargeScreenMenuSwitchMode,
    onMenuSelect: () -> Unit,
    onAddRenderItem: () -> Unit
) {
    ExpressiveButtonParent(modifier = modifier) {
        LargeScreenMenuSwitchMode.entries.forEachIndexed { index, mode ->
            ExpressiveButton(
                selected = mode == current,
                shape = expressiveButtonShape(
                    index = index,
                    size = LargeScreenMenuSwitchMode.entries.size,
                    selected = mode == current
                ),
                onClick = {
                    when (mode) {
                        LargeScreenMenuSwitchMode.Menu -> onMenuSelect()
                        LargeScreenMenuSwitchMode.AddRenderItem -> onAddRenderItem()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = when (mode) {
                            LargeScreenMenuSwitchMode.Menu -> R.drawable.ic_outline_menu_24
                            LargeScreenMenuSwitchMode.AddRenderItem -> R.drawable.ic_outlined_add_24px
                        }
                    ),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = when (mode) {
                        LargeScreenMenuSwitchMode.Menu -> stringResource(id = R.string.video_edit_bottomsheet_menu_title)
                        LargeScreenMenuSwitchMode.AddRenderItem -> stringResource(id = R.string.video_edit_bottomsheet_timeline_add_title)
                    }
                )
            }
        }
    }
}