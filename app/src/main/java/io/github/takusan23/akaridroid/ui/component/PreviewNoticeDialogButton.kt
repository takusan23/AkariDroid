package io.github.takusan23.akaridroid.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.takusan23.akaridroid.R


/** プレビューがとても遅いけど、出力には問題ないよダイアログを出すボタン */
@Composable
fun PreviewNoticeDialogButton(modifier: Modifier = Modifier) {
    val isShow = remember { mutableStateOf(false) }

    // プレビューがとても遅いけど仕様だからごめん。
    if (isShow.value) {
        AlertDialog(
            onDismissRequest = { isShow.value = false },
            title = { Text(text = stringResource(id = R.string.video_preview_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.video_preview_dialog_description)) },
            confirmButton = {
                Button(onClick = { isShow.value = false }) {
                    Text(text = stringResource(id = R.string.video_preview_dialog_close))
                }
            }
        )
    }

    IconButton(
        modifier = modifier,
        onClick = { isShow.value = !isShow.value }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_outlined_info_24px),
            contentDescription = null
        )
    }
}