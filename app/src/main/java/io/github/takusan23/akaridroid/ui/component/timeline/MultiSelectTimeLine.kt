package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.TimeLineState

@Composable
fun MultiSelectTimeLine(
    modifier: Modifier = Modifier,
    timeLineState: TimeLineState,
    currentPositionMs: () -> Long,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Boolean,
    onSeek: (positionMs: Long) -> Unit
) {
    // はみ出しているタイムラインの LayoutCoordinates
    // タイムラインのレーンや、タイムラインのアイテムの座標を出すのに必要
    val timelineScrollableAreaCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    // 選択中のアイテム
    val multiSelectedItemList = remember { mutableStateOf(emptyList<TimeLineData.Item>()) }
    // 移動中のオフセット
    val draggingOffsetOrZero = remember { mutableStateOf(IntOffset.Zero) }
    // 磁石モード用に、くっつける位置。
    // 再生位置と、今選択中の以外にくっつくように
    val magnetPositionMsList = remember(multiSelectedItemList.value, currentPositionMs()) {
        val selectedIdList = multiSelectedItemList.value.map { it.id }
        timeLineState.magnetPositionList
            .filter { it.id !in selectedIdList }
            .map { it.positionMs } + currentPositionMs()
    }

    // 横に長ーいタイムラインを作る
    RequiredSizeMultiSelectTimeLine(
        modifier = modifier
            // タイムラインにあるアイテムの座標出すのに使う
            .onGloballyPositioned { timelineScrollableAreaCoordinates.value = it },
        timeLineState = timeLineState,
        timeLineScrollableAreaCoordinates = timelineScrollableAreaCoordinates.value,
        magnetPositionMsList = magnetPositionMsList,
        multiSelectedItemList = multiSelectedItemList.value,
        onSeek = onSeek,
        onItemSelect = { item ->
            if (item in multiSelectedItemList.value) {
                multiSelectedItemList.value -= item
            } else {
                multiSelectedItemList.value += item
            }
        },
        draggingOffsetOrZero = { item ->
            if (item in multiSelectedItemList.value) {
                draggingOffsetOrZero.value
            } else {
                IntOffset.Zero
            }
        },
        onUpdateDraggingOffset = { draggingOffsetOrZero.value = it },
        onDragEnd = {
            println(it)
        }
    )
}

/**
 * 動画の長さだけずっっと横に長いタイムライン。複数選択版。[TimeLineContainer]を親にする。
 * 横と縦に長いので、親はスクロールできるようにする必要があります。
 */
@Composable
private fun RequiredSizeMultiSelectTimeLine(
    modifier: Modifier = Modifier,
    timeLineState: TimeLineState,
    timeLineScrollableAreaCoordinates: LayoutCoordinates?,
    magnetPositionMsList: List<Long>,
    multiSelectedItemList: List<TimeLineData.Item>,
    onSeek: (positionMs: Long) -> Unit,
    onItemSelect: (TimeLineData.Item) -> Unit,
    draggingOffsetOrZero: (TimeLineData.Item) -> IntOffset,
    onUpdateDraggingOffset: (IntOffset) -> Unit,
    onDragEnd: (TimeLineItemComponentDragAndDropData) -> Unit
) {
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current
    // レーンコンポーネントの位置と番号
    val timeLineLaneIndexRectMap = remember { mutableStateMapOf<Int, IntRect>() }

    Column(
        modifier = modifier
            // 画面外にはみ出すので requiredSize
            .requiredWidth(with(msWidthPx) { timeLineState.maxDurationMs.msToWidthDp })
            // 再生位置の移動。タイムラインの棒を移動させる
            .pointerInput(msWidthPx) {
                detectTapGestures {
                    val seekMs = with(msWidthPx) { it.x.toInt().widthToMs }
                    onSeek(seekMs)
                }
            },
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        // 時間を表示するやつ
        TimeLineTopTimeLabel(
            durationMs = timeLineState.maxDurationMs,
            // TODO もっといい感じにする
            stepMs = if (10 < msWidthPx.msWidthPx) {
                10_000
            } else {
                2_000
            }
        )

        HorizontalDivider()

        if (timeLineScrollableAreaCoordinates != null) {

            // タイムラインのアイテム
            // レーンの数だけ
            timeLineState.visibleTimeLineItemMap.forEach { (laneIndex, itemList) ->
                MultiSelectTimeLineLane(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .onGloballyPositioned {
                            timeLineLaneIndexRectMap[laneIndex] = timeLineScrollableAreaCoordinates
                                .localBoundingBoxOf(it)
                                .roundToIntRect()
                        },
                    laneIndex = laneIndex,
                    laneItemList = itemList,
                    timeLineScrollableAreaCoordinates = timeLineScrollableAreaCoordinates,
                    magnetPositionMsList = magnetPositionMsList,
                    multiSelectedItemList = multiSelectedItemList,
                    onItemSelect = onItemSelect,
                    draggingOffsetOrZero = draggingOffsetOrZero,
                    onUpdateDraggingOffset = onUpdateDraggingOffset,
                    onDragEnd = onDragEnd
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MultiSelectTimeLineLane(
    modifier: Modifier = Modifier,
    laneItemList: List<TimeLineData.Item>,
    laneIndex: Int,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    magnetPositionMsList: List<Long>,
    multiSelectedItemList: List<TimeLineData.Item>,
    onItemSelect: (TimeLineData.Item) -> Unit,
    draggingOffsetOrZero: (TimeLineData.Item) -> IntOffset,
    onUpdateDraggingOffset: (IntOffset) -> Unit,
    onDragEnd: (TimeLineItemComponentDragAndDropData) -> Unit
) {
    Box(modifier = modifier) {

        Text(
            text = (laneIndex + 1).toString(),
            modifier = Modifier
                .padding(start = 10.dp)
                .alpha(0.5f)
                .align(Alignment.CenterStart),
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary
        )

        laneItemList.forEach { timeLineItemData ->
            MultiSelectTimeLineItem(
                timeLineItemData = timeLineItemData,
                timeLineScrollableAreaCoordinates = timeLineScrollableAreaCoordinates,
                isSelected = timeLineItemData in multiSelectedItemList,
                magnetPositionMsList = magnetPositionMsList,
                onItemSelect = onItemSelect,
                draggingOffsetOrZero = draggingOffsetOrZero,
                onUpdateDraggingOffset = onUpdateDraggingOffset,
                onDragEnd = onDragEnd
            )
        }
    }
}