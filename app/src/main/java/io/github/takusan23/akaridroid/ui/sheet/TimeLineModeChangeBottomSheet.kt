package io.github.takusan23.akaridroid.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem
import io.github.takusan23.akaridroid.ui.component.SheetHeader

/**
 * タイムラインのモード切り替えボトムシート
 *
 * @param onDefaultClick 通常モード
 * @param onMultiSelectClick 複数選択モード
 * @param onCloseClick 閉じるを押したとき
 */
@Composable
fun TimeLineModeChangeBottomSheet(
    onDefaultClick: () -> Unit,
    onMultiSelectClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        SheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_timeline_mode_change_title),
            onClose = onCloseClick
        )

        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_timeline_mode_change_default_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_timeline_mode_change_default_description),
            iconResId = R.drawable.ic_align_horizontal_left_24px,
            onClick = onDefaultClick
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_timeline_mode_change_multi_select_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_timeline_mode_change_multi_select_description),
            iconResId = R.drawable.check_box_24px,
            onClick = onMultiSelectClick
        )
    }
}