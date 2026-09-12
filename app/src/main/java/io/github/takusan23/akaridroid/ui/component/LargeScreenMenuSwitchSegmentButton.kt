package io.github.takusan23.akaridroid.ui.component

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.takusan23.akaridroid.R

/** メニュー選択結果 */
enum class LargeScreenMenuSwitchSegmentMode {
    /** メニュー。エンコードとかの */
    Menu,

    /** 追加。動画や文字 */
    AddRenderItem
}

/**
 * 大画面の時に隣に出るメニューの切り替え UI
 *
 * @param modifier [Modifier]
 * @param current 今選択してる値
 * @param onMenuSelect メニューを押したとき
 * @param onAddRenderItem 追加を押したとき
 */
@Composable
fun LargeScreenMenuSwitchSegmentButton(
    modifier: Modifier = Modifier,
    current: LargeScreenMenuSwitchSegmentMode,
    onMenuSelect: () -> Unit,
    onAddRenderItem: () -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        LargeScreenMenuSwitchSegmentMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == current,
                onClick = {
                    when (mode) {
                        LargeScreenMenuSwitchSegmentMode.Menu -> onMenuSelect()
                        LargeScreenMenuSwitchSegmentMode.AddRenderItem -> onAddRenderItem()
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index, LargeScreenMenuSwitchSegmentMode.entries.size),
                icon = { /* do nothing */ }
            ) {
                Text(
                    text = when (mode) {
                        LargeScreenMenuSwitchSegmentMode.Menu -> stringResource(id = R.string.video_edit_bottomsheet_menu_title)
                        LargeScreenMenuSwitchSegmentMode.AddRenderItem -> stringResource(id = R.string.video_edit_bottomsheet_timeline_add_title)
                    }
                )
            }
        }
    }
}