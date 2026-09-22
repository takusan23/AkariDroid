package io.github.takusan23.akaridroid.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuSwitchItem
import io.github.takusan23.akaridroid.ui.component.SheetHeader
import io.github.takusan23.akaridroid.ui.component.data.RoundedListEndShape
import io.github.takusan23.akaridroid.ui.component.data.RoundedListSingleShape
import io.github.takusan23.akaridroid.ui.component.data.RoundedListTopShape

/**
 * タイムラインのモード切り替えボトムシート
 *
 * @param enableResize サイズ変更機能を有効にするか
 * @param onDefaultClick 通常モード
 * @param onMultiSelectClick 複数選択モード
 * @param onCloseClick 閉じるを押したとき
 * @param onResizeChangeClick サイズ変更機能の有効化切り替え
 */
@Composable
fun TimeLineMenuBottomSheet(
    enableResize: Boolean,
    onDefaultClick: () -> Unit,
    onMultiSelectClick: () -> Unit,
    onCloseClick: () -> Unit,
    onResizeChangeClick: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        SheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_timeline_menu_title),
            onClose = onCloseClick
        )

        Text(
            text = stringResource(id = R.string.video_edit_bottomsheet_timeline_menu_mode_title),
            color = MaterialTheme.colorScheme.primary
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_timeline_menu_mode_default_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_timeline_menu_mode_default_description),
            iconResId = R.drawable.ic_align_horizontal_left_24px,
            shape = RoundedListTopShape,
            onClick = onDefaultClick
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_timeline_menu_mode_multi_select_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_timeline_menu_mode_multi_select_description),
            iconResId = R.drawable.check_box_24px,
            shape = RoundedListEndShape,
            onClick = onMultiSelectClick
        )

        Text(
            text = stringResource(R.string.video_edit_bottomsheet_timeline_menu_setting_title),
            color = MaterialTheme.colorScheme.primary
        )
        BottomSheetMenuSwitchItem(
            title = stringResource(R.string.video_edit_bottomsheet_timeline_menu_setting_resize_title),
            description = stringResource(R.string.video_edit_bottomsheet_timeline_menu_setting_resize_description),
            iconResId = R.drawable.ic_outlined_touch_app_24px,
            shape = RoundedListSingleShape,
            checked = enableResize,
            onCheckedChange = onResizeChangeClick
        )
    }
}