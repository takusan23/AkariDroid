package io.github.takusan23.akaridroid.ui.sheet

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
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem
import io.github.takusan23.akaridroid.ui.component.MessageCard
import io.github.takusan23.akaridroid.ui.component.SheetHeader
import io.github.takusan23.akaridroid.ui.component.data.RoundedListEndShape
import io.github.takusan23.akaridroid.ui.component.data.RoundedListInnerShape
import io.github.takusan23.akaridroid.ui.component.data.RoundedListTopShape

/**
 * メニューボトムシート
 *
 * @param onCloseClick ヘッダーの閉じるを押したとき
 * @param onEncodeClick 動画を保存する画面（エンコード画面）を開く
 * @param onSaveVideoFrameClick 画像として保存するを押した
 * @param onVideoInfoClick 動画情報の編集画面を開く
 * @param onSettingClick 設定画面を開く
 * @param onTimeLineReset タイムラインのリセット
 */
@Composable
fun MenuBottomSheet(
    onCloseClick: () -> Unit,
    onVideoInfoClick: () -> Unit,
    onEncodeClick: () -> Unit,
    onSaveVideoFrameClick: () -> Unit,
    onTimeLineReset: () -> Unit,
    onSettingClick: () -> Unit
) {
    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        SheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_menu_title),
            onClose = onCloseClick
        )

        MessageCard(
            message = stringResource(id = R.string.video_edit_bottomsheet_menu_hint)
        )

        MenuSheet(
            onVideoInfoClick = onVideoInfoClick,
            onEncodeClick = onEncodeClick,
            onSaveVideoFrameClick = onSaveVideoFrameClick,
            onTimeLineReset = onTimeLineReset,
            onSettingClick = onSettingClick
        )
    }
}

/**
 * メニューだけ版
 *
 * @param onEncodeClick 動画を保存する画面（エンコード画面）を開く
 * @param onSaveVideoFrameClick 画像として保存するを押した
 * @param onVideoInfoClick 動画情報の編集画面を開く
 * @param onSettingClick 設定画面を開く
 * @param onTimeLineReset タイムラインのリセット
 */
@Composable
fun MenuSheet(
    modifier: Modifier = Modifier,
    onVideoInfoClick: () -> Unit,
    onEncodeClick: () -> Unit,
    onSaveVideoFrameClick: () -> Unit,
    onTimeLineReset: () -> Unit,
    onSettingClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_menu_edit_video_info_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_menu_edit_video_info_description),
            iconResId = R.drawable.ic_outline_video_file_24,
            shape = RoundedListTopShape,
            onClick = onVideoInfoClick
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_menu_encode_video_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_menu_encode_video_description),
            iconResId = R.drawable.ic_outline_save_24,
            shape = RoundedListInnerShape,
            onClick = onEncodeClick
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_menu_read_video_frame_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_menu_read_video_frame_description),
            iconResId = R.drawable.ic_photo_camera_24px,
            shape = RoundedListInnerShape,
            onClick = onSaveVideoFrameClick
        )
        TimeLineResetMenuItem(
            onResetTimeLine = onTimeLineReset
        )
        BottomSheetMenuItem(
            title = stringResource(id = R.string.video_edit_bottomsheet_menu_open_setting_title),
            description = stringResource(id = R.string.video_edit_bottomsheet_menu_open_setting_description),
            iconResId = R.drawable.ic_outline_settings_24px,
            shape = RoundedListEndShape,
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
        shape = RoundedListInnerShape,
        onClick = { isVisibleDialog.value = true }
    )
}