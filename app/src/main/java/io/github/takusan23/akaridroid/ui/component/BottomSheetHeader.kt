package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R

/**
 * ボトムシートの共通しているヘッダー
 *
 * @param modifier [Modifier]
 * @param title タイトル
 * @param onComplete 完了を押したとき
 * @param onDelete 削除押したとき
 */
@Composable
fun BottomSheetHeader(
    modifier: Modifier = Modifier,
    title: String,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            fontSize = 24.sp
        )
        OutlinedButton(onClick = onDelete) {
            Text(text = stringResource(id = R.string.bottomsheet_header_delete))
        }
        Button(onClick = onComplete) {
            Text(text = stringResource(id = R.string.bottomsheet_header_done))
        }
    }
}

/**
 * ボトムシートの共通しているヘッダー
 * 削除無し
 *
 * @param modifier [Modifier]
 * @param title タイトル
 * @param onComplete 完了を押したとき
 */
@Composable
fun BottomSheetHeader(
    modifier: Modifier = Modifier,
    title: String,
    onComplete: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            fontSize = 24.sp
        )
        Button(onClick = onComplete) {
            Text(text = stringResource(id = R.string.bottomsheet_header_done))
        }
    }
}