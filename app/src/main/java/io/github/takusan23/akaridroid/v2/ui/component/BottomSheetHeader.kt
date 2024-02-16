package io.github.takusan23.akaridroid.v2.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ボトムシートの共通しているヘッダー
 *
 * @param modifier [Modifier]
 * @param title タイトル
 * @param isEdit 編集なら true
 * @param onCreateOrUpdate 作成押したとき
 * @param onDelete 削除押したとき
 */
@Composable
fun BottomSheetHeader(
    modifier: Modifier = Modifier,
    title: String,
    isEdit: Boolean,
    onCreateOrUpdate: () -> Unit,
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
        if (isEdit) {
            OutlinedButton(onClick = onDelete) {
                Text(text = "削除")
            }
        }
        Button(onClick = onCreateOrUpdate) {
            Text(text = if (isEdit) "完了" else "追加")
        }
    }
}