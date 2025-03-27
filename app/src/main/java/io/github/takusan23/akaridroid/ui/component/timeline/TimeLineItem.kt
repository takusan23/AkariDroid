package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData

/**
 * タイムラインのアイテムの共通部分
 *
 * @param modifier [Modifier]
 * @param timeLineItemData タイムラインのアイテム情報
 * @param durationMs アイテムの長さ
 * @param timeLineScrollableAreaCoordinates タイムライン View の大きさ
 * @param onItemClick 押したとき
 * @param onDragStart ドラッグアンドドロップ開始時。
 * @param onDragProgress ドラッグアンドドロップで移動中
 * @param onDragEnd ドラッグアンドドロップ終了
 * @param itemSuffix アイテムの最後に何か付けるなら
 */
@Composable
fun BaseTimeLineItem(
    modifier: Modifier = Modifier,
    timeLineItemData: TimeLineData.Item,
    durationMs: Long = timeLineItemData.durationMs,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    onItemClick: () -> Unit,
    onDragStart: (IntRect) -> Unit,
    onDragProgress: (x: Float, y: Float) -> Unit,
    onDragEnd: (start: IntRect, end: IntRect) -> Unit,
    itemSuffix: @Composable BoxScope.() -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    // 拡大縮小
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current
    // タイムラインから見た自分の位置
    val latestGlobalRect = remember { mutableStateOf(IntRect.Zero) }

    Surface(
        modifier = modifier
            .width(with(msWidthPx) { durationMs.msToWidthDp })
            .fillMaxHeight()
            .onGloballyPositioned {
                // timeLineScrollableAreaCoordinates の理由は TimeLine() コンポーネント参照
                latestGlobalRect.value = timeLineScrollableAreaCoordinates
                    .localBoundingBoxOf(it)
                    .roundToIntRect()
            }
            .pointerInput(timeLineItemData) {
                // 開始時の IntRect を持っておく
                var startIntRect = IntRect.Zero
                detectDragGestures(
                    onDragStart = {
                        // ドラッグアンドドロップ開始時。フラグを立てて開始位置を入れておく
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        startIntRect = latestGlobalRect.value
                        onDragStart(latestGlobalRect.value)
                    },
                    onDrag = { change, dragAmount ->
                        // 移動中
                        change.consume()
                        onDragProgress(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        // 移動終了
                        onDragEnd(startIntRect, latestGlobalRect.value)
                    }
                )
            },
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        onClick = onItemClick
    ) {
        Box(modifier = Modifier.height(IntrinsicSize.Max)) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    modifier = Modifier.padding(5.dp),
                    painter = painterResource(id = timeLineItemData.iconResId),
                    contentDescription = null
                )
                Text(
                    text = timeLineItemData.label,
                    maxLines = 1
                )
            }
            // 長さ調整できる場合は、それ用のつまみを出す
            itemSuffix()
        }
    }
}
