package io.github.takusan23.akaridroid.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.takusan23.akaridroid.R

/**
 * ダイアログ
 *
 * @param modifier [Modifier]
 * @param title タイトル
 * @param message 本文
 * @param onClose 閉じる時
 */
@Composable
fun CommonDialog(
    modifier: Modifier = Modifier,
    title: String,
    message: String? = null,
    onClose: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onClose,
        title = { Text(text = title) },
        text = if (message != null) {
            { Text(text = message) }
        } else null,
        confirmButton = {
            Button(onClick = onClose) {
                Text(text = stringResource(id = R.string.video_preview_dialog_close))
            }
        }
    )
}