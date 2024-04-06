package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem
import io.github.takusan23.akaridroid.ui.component.MessageCard

/**
 * メニューボトムシート
 *
 * @param onEncodeClick 動画を保存する画面（エンコード画面）を開く
 * @param onVideoInfoClick 動画情報の編集画面を開く
 * @param onSettingClick 設定画面を開く
 * @param onTimeLineReset タイムラインのリセット
 */
@Composable
fun MenuBottomSheet(
    onVideoInfoClick: () -> Unit,
    onEncodeClick: () -> Unit,
    onTimeLineReset: () -> Unit,
    onSettingClick: () -> Unit
) {
    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = stringResource(id = R.string.video_edit_bottomsheet_menu_title),
            fontSize = 24.sp
        )

        MessageCard(
            message = stringResource(id = R.string.video_edit_bottomsheet_menu_hint)
        )

        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_menu_edit_video_info_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_menu_edit_video_info_description),
            iconResId = R.drawable.ic_outline_video_file_24,
            onClick = onVideoInfoClick
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_menu_encode_video_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_menu_encode_video_description),
            iconResId = R.drawable.ic_outline_save_24,
            onClick = onEncodeClick
        )
        TimeLineResetMenuItem(
            onResetTimeLine = onTimeLineReset
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_menu_open_setting_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_menu_open_setting_description),
            iconResId = R.drawable.ic_outline_settings_24px,
            onClick = onSettingClick
        )
    }
}

/**
 * タイムラインリセットメニュー。押したらダイアログが出て本当にやるか聞かれます。
 *
 * @param onResetTimeLine ダイアログで破棄を選んだとき
 */
@Composable
private fun TimeLineResetMenuItem(onResetTimeLine: () -> Unit) {
    val isVisibleDialog = remember { mutableStateOf(false) }

    if (isVisibleDialog.value) {
        AlertDialog(
            onDismissRequest = { isVisibleDialog.value = false },
            icon = { Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null) },
            title = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_menu_reset_timeline_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_menu_reset_timeline_dialog_message)) },
            dismissButton = {
                TextButton(onClick = { isVisibleDialog.value = false }) {
                    Text(text = stringResource(id = R.string.video_edit_bottomsheet_menu_reset_timeline_dialog_cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetTimeLine()
                        isVisibleDialog.value = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.video_edit_bottomsheet_menu_reset_timeline_dialog_ok))
                }
            }
        )
    }

    BottomSheetMenuItem(
        title = stringResource(id = R.string.video_edit_bottomsheet_menu_reset_timeline_title),
        description = stringResource(id = R.string.video_edit_bottomsheet_menu_reset_timeline_description),
        iconResId = R.drawable.ic_outline_reset_wrench_24px,
        onClick = { isVisibleDialog.value = true }
    )
}