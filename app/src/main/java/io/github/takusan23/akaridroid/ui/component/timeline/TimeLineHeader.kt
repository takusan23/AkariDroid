package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import kotlin.math.roundToInt

/**
 * タイムラインのデフォルトヘッダー
 * [UndoRedoButtons]と[TimeLineZoomButtons]参照。
 */
@Composable
fun DefaultTimeLineHeader(
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean,
    msWidthPx: Int,
    onModeChangeClick: () -> Unit,
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
        TextButton(onClick = onModeChangeClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_sync_24dp),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.timeline_header_default_mode_switch))
        }

        if (fillMaxWidth) {
            Spacer(modifier = Modifier.weight(1f))
        }

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
    fillMaxWidth: Boolean,
    onExitMultiSelect: () -> Unit,
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

        IconButton(onClick = onExitMultiSelect) {
            Icon(painter = painterResource(R.drawable.ic_outline_close_24), contentDescription = null)
        }

        Column {
            val lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
            Text(
                text = stringResource(id = R.string.timeline_header_multi_select_multi_select_title),
                style = TextStyle(lineHeightStyle = lineHeightStyle)
            )
            Text(
                text = stringResource(id = R.string.timeline_header_multi_select_multi_select_description),
                fontSize = 12.sp,
                style = TextStyle(lineHeightStyle = lineHeightStyle)
            )
        }

        if (fillMaxWidth) {
            Spacer(modifier = Modifier.weight(1f))
        }

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
 * 大画面の時はヘッダーをドラッグで移動できるようにするので
 *
 * @param content [MultiSelectTimeLineHeader]か[DefaultTimeLineHeader]
 */
@Composable
fun LargeScreenDefaultTimeLineHeader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { mutableFloatStateOf(0f) }
    val offsetY = remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier.offset {
            IntOffset(
                x = offsetX.floatValue.roundToInt(),
                y = offsetY.floatValue.roundToInt()
            )
        },
        color = color,
        shape = CircleShape,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(
                vertical = 5.dp, // つかみやすいように増やす
                horizontal = 10.dp // 角が丸いので
            ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier.pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        offsetX.floatValue += dragAmount.x
                        offsetY.floatValue += dragAmount.y
                    }
                },
                onClick = { /* do nothing ripple のために... */ }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_menu_24),
                    contentDescription = null
                )
            }
            content()
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