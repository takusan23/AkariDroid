package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem

/**
 * タイムラインのモード切り替えボトムシート
 *
 * @param onDefaultClick 通常モード
 * @param onMultiSelectClick 複数選択モード
 */
@Composable
fun TimeLineModeChangeBottomSheet(
    onDefaultClick: () -> Unit,
    onMultiSelectClick: () -> Unit
) {
    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = stringResource(id = R.string.video_edit_bottomsheet_timeline_mode_change_title),
            fontSize = 24.sp
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