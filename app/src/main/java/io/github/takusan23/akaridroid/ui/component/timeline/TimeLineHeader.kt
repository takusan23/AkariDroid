package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R

/**
 * タイムラインのデフォルトヘッダー
 * [UndoRedoButtons]と[TimeLineZoomButtons]参照。
 */
@Composable
fun DefaultTimeLineHeader(
    modifier: Modifier = Modifier,
    msWidthPx: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    hasUndo: Boolean,
    hasRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))

        ZoomHistoryButtons(
            msWidthPx = msWidthPx,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            hasUndo = hasUndo,
            hasRedo = hasRedo,
            onUndo = onUndo,
            onRedo = onRedo
        )
    }
}

/**
 * 複数選択モード時のヘッダー。
 * 残りは[UndoRedoButtons]と[TimeLineZoomButtons]参照。
 *
 * @param modifier [Modifier]
 * @param onExitMultiSelect 複数選択モードを閉じるとき
 */
@Composable
fun MultiSelectTimeLineHeader(
    modifier: Modifier = Modifier,
    onExitMultiSelect: () -> Unit,
    msWidthPx: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    hasUndo: Boolean,
    hasRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        modifier = modifier,
        // 複数選択モード時の elevation、TopBar() もこれくらいやろ
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(onClick = onExitMultiSelect) {
                Icon(painter = painterResource(R.drawable.ic_outline_close_24), contentDescription = null)
            }

            Column {
                val lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
                Text(
                    text = "複数選択モード",
                    style = TextStyle(lineHeightStyle = lineHeightStyle)
                )
                Text(
                    text = "移動やコピー、削除ができます",
                    fontSize = 12.sp,
                    style = TextStyle(lineHeightStyle = lineHeightStyle)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ZoomHistoryButtons(
                msWidthPx = msWidthPx,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                hasUndo = hasUndo,
                hasRedo = hasRedo,
                onUndo = onUndo,
                onRedo = onRedo
            )
        }
    }
}

/** [DefaultTimeLineHeader]と[MultiSelectTimeLineHeader]の共通部分 */
@Composable
private fun ZoomHistoryButtons(
    modifier: Modifier = Modifier,
    msWidthPx: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    hasUndo: Boolean,
    hasRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeLineZoomButtons(
            msWidthPx = msWidthPx,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut
        )
        UndoRedoButtons(
            hasUndo = hasUndo,
            hasRedo = hasRedo,
            onUndo = onUndo,
            onRedo = onRedo
        )
    }
}