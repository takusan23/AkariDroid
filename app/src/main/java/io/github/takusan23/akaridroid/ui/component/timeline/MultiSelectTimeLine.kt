package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.TimeLineState
import kotlin.math.abs

/**
 * 複数選択モードのタイムライン
 *
 * @param modifier [Modifier]
 * @param timeLineState [io.github.takusan23.akaridroid.ui.component.data.rememberTimeLineState]
 * @param selectedItemIdList 選択中のアイテムの ID 一覧。ID で持っている理由は、時間を変更するとデータクラスの中身が変わってしまうから
 * @param currentPositionMs プレビューの再生位置
 * @param onItemSelect 選択時
 * @param onSeek プレビューのシーク時に呼ばれる
 * @param onDragAndDropRequest アイテムのドラッグアンドドロップが要求されたとき
 */
@Composable
fun MultiSelectTimeLine(
    modifier: Modifier = Modifier,
    timeLineState: TimeLineState,
    currentPositionMs: () -> Long,
    selectedItemIdList: List<Long>,
    onItemSelect: (TimeLineData.Item) -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onDragAndDropRequest: (request: List<TimeLineData.DragAndDropRequest>) -> Unit
) {
    // はみ出しているタイムラインの LayoutCoordinates
    // タイムラインのレーンや、タイムラインのアイテムの座標を出すのに必要
    val timelineScrollableAreaCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    // 横に長ーいタイムラインを作る
    RequiredSizeMultiSelectTimeLine(
        modifier = modifier
            // タイムラインにあるアイテムの座標出すのに使う
            .onGloballyPositioned { timelineScrollableAreaCoordinates.value = it },
        selectedItemIdList = selectedItemIdList,
        timeLineState = timeLineState,
        timeLineScrollableAreaCoordinates = timelineScrollableAreaCoordinates.value,
        onItemSelect = onItemSelect,
        onSeek = onSeek,
        currentPositionMs = currentPositionMs,
        onDragAndDropRequest = onDragAndDropRequest
    )
}

/**
 * 動画の長さだけずっっと横に長いタイムライン。複数選択版。[TimeLineContainer]を親にする。
 * 横と縦に長いので、親はスクロールできるようにする必要があります。
 *
 * @param modifier [Modifier]
 * @param timeLineState [io.github.takusan23.akaridroid.ui.component.data.rememberTimeLineState]
 * @param selectedItemIdList 選択中のアイテムの ID 一覧。ID で持っている理由は、時間を変更するとデータクラスの中身が変わってしまうから
 * @param timeLineScrollableAreaCoordinates タイムラインの View の大きさ
 * @param currentPositionMs プレビューの再生位置
 * @param onItemSelect 選択時
 * @param onSeek プレビューのシーク時に呼ばれる
 * @param onDragAndDropRequest アイテムのドラッグアンドドロップが要求されたとき
 */
@Composable
private fun RequiredSizeMultiSelectTimeLine(
    modifier: Modifier = Modifier,
    timeLineState: TimeLineState,
    selectedItemIdList: List<Long>,
    timeLineScrollableAreaCoordinates: LayoutCoordinates?,
    currentPositionMs: () -> Long,
    onItemSelect: (TimeLineData.Item) -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onDragAndDropRequest: (request: List<TimeLineData.DragAndDropRequest>) -> Unit
) {
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current
    // レーンコンポーネントの位置と番号
    val timeLineLaneIndexRectMap = remember { mutableStateMapOf<Int, IntRect>() }

    // Haptics 制御
    // 移動開始、磁石モード発動時にフィードバックを
    val haptic = LocalHapticFeedback.current
    val isHapticEnable = remember { mutableStateOf(true) }

    // 移動中のオフセット。選択中のアイテムはこれを足す
    val draggingOffset = remember { mutableStateOf(IntOffset.Zero) }

    // pointerInput() は値が更新されないので
    val latestSelectedItemIdList = rememberUpdatedState(selectedItemIdList)
    // 移動開始位置
    val dragStartPosition = remember(selectedItemIdList) { mutableStateOf(emptyMap<Long, IntRect?>()) }
    // タイムライン上のアイテムの座標を持っておく。移動開始と終了で参照する
    val timeLineItemPositionMap = remember { mutableStateOf(emptyMap<Long, IntRect>()) }

    // 磁石モード用に、くっつける位置。
    // 再生位置と、今選択中の以外にくっつくように
    val magnetPositionMsList = remember(selectedItemIdList, currentPositionMs()) {
        timeLineState.magnetPositionList
            .filter { it.id !in selectedItemIdList }
            .map { it.positionMs } + currentPositionMs()
    }

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
                    isSelected = { it.id in selectedItemIdList },
                    onItemSelect = onItemSelect,
                    draggingOffset = { offsetItem ->
                        val originPosition = IntOffset(with(msWidthPx) { offsetItem.startMs.msToWidth }, 0)
                        if (offsetItem.id in selectedItemIdList) {
                            draggingOffset.value + originPosition
                        } else {
                            originPosition
                        }
                    },
                    onPositionChanged = { positionChangeItem, positionIntRect ->
                        timeLineItemPositionMap.value += positionChangeItem.id to positionIntRect
                    },
                    onDragStart = { _, _ ->
                        // ドラッグアンドドロップ開始時。フラグを立てて開始位置を入れておく
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        draggingOffset.value = IntOffset.Zero

                        // 移動開始時に初期値を入れる
                        dragStartPosition.value = latestSelectedItemIdList.value.associateWith { itemId ->
                            timeLineItemPositionMap.value[itemId] ?: return@MultiSelectTimeLineLane
                        }
                    },
                    onDragProgress = { draggingItem, x, y ->
                        // 移動させる
                        val updatePos = IntOffset(
                            x = (draggingOffset.value.x + x).toInt(),
                            y = (draggingOffset.value.y + y).toInt()
                        )
                        draggingOffset.value = updatePos
                        isHapticEnable.value = true
                        // 移動前の時間
                        val beforeStartMs = draggingItem.startMs
                        // updatePos は移動開始が Zero なので、元の値を足す
                        val currentOffset = beforeStartMs + with(msWidthPx) { updatePos.x.widthToMs }
                        // 磁石モード発動するか
                        val magnetPositionMsOrNull = magnetPositionMsList.firstOrNull { magnetPositionMs ->
                            abs(magnetPositionMs - currentOffset) < MAGNET_THRESHOLD_MOVE
                        }
                        if (magnetPositionMsOrNull != null) {
                            // 差があるときだけにして連続対策
                            if (isHapticEnable.value) {
                                isHapticEnable.value = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            // Offset は移動開始を Zero としているので引いておく
                            draggingOffset.value = updatePos.copy(x = with(msWidthPx) { (magnetPositionMsOrNull - beforeStartMs).msToWidth })
                        } else {
                            draggingOffset.value = updatePos
                            isHapticEnable.value = true
                        }
                    },
                    onDragEnd = { _, _, _ -> // 指で押してるやつなので使わない
                        // 移動終了
                        // 選択中のアイテムすべてに判定
                        val dragAndDropRequestList = dragStartPosition.value.mapNotNull { (id, start) ->
                            start ?: return@mapNotNull null
                            // 終了位置
                            val stop = timeLineItemPositionMap.value[id] ?: return@mapNotNull null

                            // ドラッグアンドドロップが終わった後の位置に対応する時間を出す
                            val stopMsInDroppedPos = with(msWidthPx) { stop.left.widthToMs }

                            // ドラッグアンドドロップで移動先に移動してよいか
                            // 移動元、移動先のレーン番号を取得
                            val fromLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(start.center) }.keys.firstOrNull() ?: return@mapNotNull null
                            val toLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(stop.center) }.keys.firstOrNull() ?: return@mapNotNull null

                            TimeLineData.DragAndDropRequest(
                                id = id,
                                dragAndDroppedStartMs = stopMsInDroppedPos,
                                dragAndDroppedLaneIndex = toLaneIndex
                            )
                        }

                        // ViewModel 側に投げて判定を
                        onDragAndDropRequest(dragAndDropRequestList)
                        draggingOffset.value = IntOffset.Zero
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

/**
 * タイムラインのレーン
 * ドラッグアンドドロップのコールバック系は、複数選択だとしても、指で押してるアイテムのみが呼ばれます。
 *
 * @param modifier [Modifier]
 * @param laneItemList そのレーンのアイテム一覧
 * @param laneIndex レーン番号
 * @param timeLineScrollableAreaCoordinates タイムライン View の大きさ
 * @param onItemSelect 押したとき
 * @param isSelected 複数選択中なら true
 * @param draggingOffset 移動するなら Offset
 * @param onPositionChanged タイムラインの各アイテムの位置変更時に呼ばれる
 * @param onDragStart ドラッグアンドドロップ開始時。
 * @param onDragProgress ドラッグアンドドロップで移動中
 * @param onDragEnd ドラッグアンドドロップ終了
 */
@Composable
private fun MultiSelectTimeLineLane(
    modifier: Modifier = Modifier,
    laneItemList: List<TimeLineData.Item>,
    laneIndex: Int,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    onItemSelect: (TimeLineData.Item) -> Unit,
    isSelected: (TimeLineData.Item) -> Boolean,
    draggingOffset: (TimeLineData.Item) -> IntOffset,
    onPositionChanged: (TimeLineData.Item, IntRect) -> Unit,
    onDragStart: (TimeLineData.Item, IntRect) -> Unit,
    onDragProgress: (TimeLineData.Item, x: Float, y: Float) -> Unit,
    onDragEnd: (TimeLineData.Item, start: IntRect, end: IntRect) -> Unit
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
                isSelected = isSelected(timeLineItemData),
                onItemSelect = onItemSelect,
                onPositionChanged = { onPositionChanged(timeLineItemData, it) },
                draggingOffset = { draggingOffset(timeLineItemData) },
                onDragStart = { startRect -> onDragStart(timeLineItemData, startRect) },
                onDragProgress = { x, y -> onDragProgress(timeLineItemData, x, y) },
                onDragEnd = { start, end -> onDragEnd(timeLineItemData, start, end) }
            )
        }
    }
}

/**
 * 複数選択可能なタイムラインの各アイテム
 *
 * @param modifier [Modifier]
 * @param timeLineItemData タイムラインのアイテム情報
 * @param durationMs アイテムの長さ
 * @param timeLineScrollableAreaCoordinates タイムライン View の大きさ
 * @param isSelected 選択中なら true
 * @param onItemSelect 押したとき
 * @param draggingOffset 移動するなら Offset
 * @param onPositionChanged タイムラインの各アイテムの位置変更時に呼ばれる
 * @param onDragStart ドラッグアンドドロップ開始時。
 * @param onDragProgress ドラッグアンドドロップで移動中
 * @param onDragEnd ドラッグアンドドロップ終了
 */
@Composable
private fun MultiSelectTimeLineItem(
    modifier: Modifier = Modifier,
    timeLineItemData: TimeLineData.Item,
    durationMs: Long = timeLineItemData.durationMs,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    isSelected: Boolean,
    onItemSelect: (TimeLineData.Item) -> Unit,
    draggingOffset: (TimeLineData.Item) -> IntOffset,
    onPositionChanged: (IntRect) -> Unit,
    onDragStart: (IntRect) -> Unit,
    onDragProgress: (x: Float, y: Float) -> Unit,
    onDragEnd: (start: IntRect, end: IntRect) -> Unit
) {
    BaseTimeLineItem(
        modifier = modifier
            .offset { draggingOffset(timeLineItemData) }
            .onGloballyPositioned {
                onPositionChanged(
                    timeLineScrollableAreaCoordinates
                        .localBoundingBoxOf(it)
                        .roundToIntRect()
                )
            },
        timeLineItemData = timeLineItemData,
        durationMs = durationMs,
        timeLineScrollableAreaCoordinates = timeLineScrollableAreaCoordinates,
        onItemClick = { onItemSelect(timeLineItemData) },
        onDragStart = onDragStart,
        onDragProgress = onDragProgress,
        onDragEnd = onDragEnd,
        itemSuffix = {
            // TODO 画像に差し替える
            if (isSelected) {
                Checkbox(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .align(Alignment.CenterEnd),
                    checked = true,
                    onCheckedChange = null
                )
            }
        }
    )
}
