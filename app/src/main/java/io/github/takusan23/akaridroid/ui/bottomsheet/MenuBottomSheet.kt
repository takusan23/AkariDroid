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
            text = "メニュー",
            fontSize = 24.sp
        )

        MessageCard(message = "タイムラインへの素材追加はメニューの隣のボタンからできます")

        BottomSheetMenuItem(
            title = "動画情報の編集",
            description = "動画の解像度（縦横サイズ）はここで変更できます。",
            iconResId = R.drawable.ic_outline_video_file_24,
            onClick = onVideoInfoClick
        )
        BottomSheetMenuItem(
            title = "動画を保存する",
            description = "保存（書き出し、エンコード）をします。",
            iconResId = R.drawable.ic_outline_save_24,
            onClick = onEncodeClick
        )
        TimeLineResetMenuItem(onTimeLineReset = onTimeLineReset)
        BottomSheetMenuItem(
            title = "設定",
            description = "好きなフォントを取り込むとか、オープンソースライセンスとか。",
            iconResId = R.drawable.ic_outline_settings_24px,
            onClick = onSettingClick
        )
    }
}

/** タイムラインリセットメニュー。押したらダイアログが出て本当にやるか聞かれます。 */
@Composable
private fun TimeLineResetMenuItem(onTimeLineReset: () -> Unit) {
    val isVisibleDialog = remember { mutableStateOf(false) }

    if (isVisibleDialog.value) {
        AlertDialog(
            onDismissRequest = { isVisibleDialog.value = false },
            icon = { Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null) },
            title = { Text(text = "本当に破棄しますか") },
            text = { Text(text = "タイムラインの素材を全て破棄して、まっさらな状態にします。本当に破棄しますか？") },
            dismissButton = {
                TextButton(onClick = { isVisibleDialog.value = false }) {
                    Text(text = "戻る")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onTimeLineReset()
                        isVisibleDialog.value = false
                    }
                ) {
                    Text(text = "破棄する")
                }
            }
        )
    }

    BottomSheetMenuItem(
        title = "タイムラインをリセットする",
        description = "タイムラインにある素材をすべて消します。",
        iconResId = R.drawable.ic_outline_reset_wrench_24px,
        onClick = { isVisibleDialog.value = true }
    )
}