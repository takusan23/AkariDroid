package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
 * @param onClose バツを押したとき
 */
@Composable
fun SheetHeader(
    modifier: Modifier = Modifier,
    title: String,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
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
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_close_24),
                contentDescription = null
            )
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
 * @param onClose バツを押したとき
 */
@Composable
fun SheetHeader(
    modifier: Modifier = Modifier,
    title: String,
    onComplete: () -> Unit,
    onClose: () -> Unit
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
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_close_24),
                contentDescription = null
            )
        }
    }
}

/**
 * ボトムシートの共通しているヘッダー
 * 閉じるだけ
 *
 * @param modifier [Modifier]
 * @param title タイトル
 * @param onClose バツを押したとき
 */
@Composable
fun SheetHeader(
    modifier: Modifier = Modifier,
    title: String,
    onClose: () -> Unit
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
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_close_24),
                contentDescription = null
            )
        }
    }
}