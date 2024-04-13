package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/**
 * 元に戻す、戻す操作を取り消すボタン。
 *
 * @param modifier [Modifier]
 * @param hasUndo 元に戻す操作が利用できるか
 * @param hasRedo 戻す操作を取り消す操作ができるか
 * @param onUndo 元に戻すを押した時に呼ばれる
 * @param onRedo 戻す操作を取り消すを押した時に呼ばれる
 */
@Composable
fun UndoRedoButtons(
    modifier: Modifier = Modifier,
    hasUndo: Boolean,
    hasRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        IconButton(
            onClick = onUndo,
            enabled = hasUndo
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_undo_24px), contentDescription = null)
        }
        IconButton(
            onClick = onRedo,
            enabled = hasRedo
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_redo_24px), contentDescription = null)
        }
    }
}