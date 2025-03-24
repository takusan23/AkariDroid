package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.TimeLineState
import kotlin.math.abs

@Composable
fun MultiSelectTimeLineItem(
    modifier: Modifier = Modifier,
    timeLineItemData: TimeLineData.Item,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    isSelected: Boolean,
    onItemSelect: (TimeLineData.Item) -> Unit,
    draggingOffsetOrZero: (TimeLineData.Item) -> IntOffset,
    onDragStart: (IntRect) -> Unit,
    onDragProgress: (x: Float, y: Float) -> Unit,
    onDragEnd: (start: IntRect, end: IntRect) -> Unit
) {
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current

    BaseTimeLineItem(
        modifier = modifier.offset {
            // 移動中の場合はそのオフセット
            // 移動中じゃない場合は Zero なので足し算しても問題ないはず
            draggingOffsetOrZero(timeLineItemData) + IntOffset(with(msWidthPx) { timeLineItemData.startMs.msToWidth }, 0)
        },
        timeLineItemData = timeLineItemData,
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

/**
 * タイムラインに表示するアイテム
 *
 * @param modifier [Modifier]
 * @param timeLineItemData タイムラインのデータ[TimeLineData.Item]
 * @param currentPositionMs 現在の再生位置（赤いバーがある位置）
 * @param timeLineScrollableAreaCoordinates 横に長いタイムラインを表示しているコンポーネントの[LayoutCoordinates]
 * @param magnetPositionList [MagnetPosition]の配列。
 * @param onDragAndDropRequest ドラッグアンドドロップで指を離したら呼ばれます。引数は[TimeLineItemComponentDragAndDropData]参照
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete 削除を押した
 * @param onDuplicate 複製を押した
 * @param onDurationChange 長さ調整がリクエストされた。長さ調整つまみを離したら呼ばれる。
 */
@Composable
fun SingleTimeLineItem(
    modifier: Modifier = Modifier,
    timeLineItemData: TimeLineData.Item,
    currentPositionMs: () -> Long,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    magnetPositionList: List<TimeLineState.MagnetPosition>,
    onDragAndDropRequest: (TimeLineItemComponentDragAndDropData) -> Unit,
    onEdit: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit
) {
    // 拡大縮小
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current
    // アイテムの移動中位置
    val draggingOffset = remember { mutableStateOf(IntOffset.Zero) }
    // メニューを表示するか
    val isVisibleMenu = remember { mutableStateOf(false) }
    // タイムラインのアイテムの表示時間。長さ調整出来るように State で持っている。
    val durationMs = remember(timeLineItemData) { mutableLongStateOf(timeLineItemData.durationMs) }

    // 磁石モード用に、くっつける位置。
    // 自分自身は除く必要あり。あと再生位置も欲しい
    val magnetPositionMsList = remember(magnetPositionList, currentPositionMs()) {
        magnetPositionList
            .filter { it.id != timeLineItemData.id }
            .map { it.positionMs } + currentPositionMs()
    }

    // Haptics 制御
    // 移動開始、磁石モード発動時にフィードバックを
    val haptic = LocalHapticFeedback.current
    val isHapticEnable = remember { mutableStateOf(true) }

    Box(
        modifier = modifier.offset {
            // 移動中の場合はそのオフセット
            // 移動中じゃない場合は Zero なので足し算しても問題ないはず
            draggingOffset.value + IntOffset(with(msWidthPx) { timeLineItemData.startMs.msToWidth }, 0)
        }
    ) {
        BaseTimeLineItem(
            timeLineItemData = timeLineItemData,
            durationMs = durationMs.longValue,
            onItemClick = { isVisibleMenu.value = true },
            timeLineScrollableAreaCoordinates = timeLineScrollableAreaCoordinates,
            onDragStart = {
                // ドラッグアンドドロップ開始時。フラグを立てて開始位置を入れておく
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                draggingOffset.value = IntOffset.Zero
            },
            onDragProgress = { x, y ->
                // 移動させる
                val updatePos = IntOffset(
                    x = (draggingOffset.value.x + x).toInt(),
                    y = (draggingOffset.value.y + y).toInt()
                )
                // 移動前の時間
                val beforeStartMs = timeLineItemData.startMs
                // updatePos は移動開始が Zero なので、元の値を足す
                val currentOffset = beforeStartMs + with(msWidthPx) { updatePos.x.widthToMs }
                // 磁石モード発動するか
                val magnetPositionMsOrNull = magnetPositionMsList.firstOrNull { magnetPositionMs -> abs(magnetPositionMs - currentOffset) < MAGNET_THRESHOLD_MOVE }
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
            onDragEnd = { startRect, endRect ->
                // 移動終了
                // ドラッグアンドドロップが終わった後の位置に対応する時間を出す
                val stopMsInDroppedPos = with(msWidthPx) { endRect.left.widthToMs }
                // 移動できるか判定を上のコンポーネントでやる
                // 出来ない場合は ViewModel からもとに戻った状態のデータに上書きされるはず
                onDragAndDropRequest(
                    TimeLineItemComponentDragAndDropData(
                        id = timeLineItemData.id,
                        startRect = startRect,
                        stopRect = endRect,
                        positionMs = stopMsInDroppedPos
                    )
                )
                draggingOffset.value = IntOffset.Zero
            },
            itemSuffix = {
                if (timeLineItemData.isChangeDuration) {
                    DurationChangeHandle(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .pointerInput(timeLineItemData, msWidthPx, magnetPositionMsList) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        // 動かした位置に対応した時間を出す
                                        val widthToMs = durationMs.longValue + with(msWidthPx) { dragAmount.x.widthToMs }
                                        // 磁石モード。開始位置を加味して探す必要がある
                                        // startMs を引くことで、durationMs として使えるように
                                        val magnetPositionMsOrNull = magnetPositionMsList
                                            .firstOrNull { magnetPositionMs -> abs(magnetPositionMs - (timeLineItemData.startMs + widthToMs)) < MAGNET_THRESHOLD_DURATION_CHANGE }
                                            ?.let { nonnullLong -> nonnullLong - timeLineItemData.startMs }

                                        // 前回と違うときのみ Haptic
                                        if (magnetPositionMsOrNull != null && durationMs.longValue != magnetPositionMsOrNull) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            durationMs.longValue = magnetPositionMsOrNull
                                        } else {
                                            durationMs.longValue = widthToMs
                                        }
                                    },
                                    onDragEnd = {
                                        onDurationChange(
                                            TimeLineData.DurationChangeRequest(
                                                id = timeLineItemData.id,
                                                newDurationMs = durationMs.longValue
                                            )
                                        )
                                    }
                                )
                            }
                    )
                }
            }
        )

        // ドロップダウンメニュー
        // ボトムシートを出すとか
        TimeLineItemContextMenu(
            isVisibleMenu = isVisibleMenu.value,
            isEnableCut = currentPositionMs() in timeLineItemData.timeRange,
            onDismissRequest = { isVisibleMenu.value = false },
            onEdit = onEdit,
            onCut = onCut,
            onDelete = onDelete,
            onDuplicate = onDuplicate
        )
    }
}

@Composable
private fun BaseTimeLineItem(
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

/**
 * アイテムを押したときのメニューです。
 * 値の編集、この位置で分割など。
 *
 * @param isVisibleMenu 表示する場合は true
 * @param isEnableCut この位置で分割が利用できるか。赤いバーがタイムラインのアイテムに重なってないのに表示されるのはあれ
 * @param onDismissRequest 非表示にして欲しいときに呼ばれる
 * @param onEdit 値の編集を押した
 * @param onCut 分割を押した
 * @param onDelete 削除を押した
 * @param onDuplicate 複製を押した
 */
@Composable
private fun TimeLineItemContextMenu(
    isVisibleMenu: Boolean,
    isEnableCut: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    DropdownMenu(
        expanded = isVisibleMenu,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.timeline_context_menu_edit)) },
            onClick = {
                onEdit()
                onDismissRequest()
            },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_outline_edit_24px), contentDescription = null) }
        )
        if (isEnableCut) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.timeline_context_menu_cut)) },
                onClick = {
                    onCut()
                    onDismissRequest()
                },
                leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_outline_cut_24px), contentDescription = null) }
            )
        }
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.timeline_context_menu_duplicate)) },
            onClick = {
                onDuplicate()
                onDismissRequest()
            },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_content_copy_24px), contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.timeline_context_menu_delete)) },
            onClick = {
                onDelete()
                onDismissRequest()
            },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null) }
        )
    }
}


/**
 * 長さ調整用のつまみ
 * Modifier に pointerInput いれて使ってね
 *
 * @param modifier [Modifier]
 */
@Composable
private fun DurationChangeHandle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}