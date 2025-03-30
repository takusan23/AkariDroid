package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.TimeLineState
import kotlin.math.abs

/**
 * タイムラインのアイテムが移動したときに貰えるデータ
 * [startRect]、[stopRect]はレーンの判定だけに使ってください。時間をこの[IntRect]から出すと多分正確じゃない（丸められる）
 *
 * @param id [TimeLineData.Item.id]と同じ
 * @param startRect 移動前の位置
 * @param stopRect 移動後の位置
 * @param positionMs 移動後の再生開始位置
 */
private data class TimeLineItemComponentDragAndDropData(
    val id: Long,
    val startRect: IntRect,
    val stopRect: IntRect,
    val positionMs: Long
)

/**
 * デフォルトのタイムライン
 *
 * @param modifier [Modifier]
 * @param timeLineState [io.github.takusan23.akaridroid.ui.component.data.rememberTimeLineState]
 * @param currentPositionMs プレビューの再生位置
 * @param onDragAndDropRequest ドラッグアンドドロップでアイテム移動が要求されたら呼ばれる
 * @param onSeek プレビューのシーク時に呼ばれる
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete メニューで削除を押した
 * @param onDuplicate メニューで複製を押した
 * @param onCopy メニューでコピーを押した
 * @param onDurationChange 長さ調整がリクエストされた
 */
@Composable
fun DefaultTimeLine(
    modifier: Modifier = Modifier,
    timeLineState: TimeLineState,
    currentPositionMs: () -> Long,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit, // TODO これ TimeLineData.Item 全部のパラメーターは要らないわ。
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onCopy: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit
) {
    // はみ出しているタイムラインの LayoutCoordinates
    // タイムラインのレーンや、タイムラインのアイテムの座標を出すのに必要
    val timelineScrollableAreaCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    RequiredSizeTimeLine(
        modifier = modifier
            // タイムラインにあるアイテムの座標出すのに使う
            .onGloballyPositioned { timelineScrollableAreaCoordinates.value = it },
        timeLineState = timeLineState,
        onSeek = onSeek,
        timeLineScrollableAreaCoordinates = timelineScrollableAreaCoordinates.value,
        currentPositionMs = currentPositionMs,
        onEdit = onEdit,
        onCut = onCut,
        onDelete = onDelete,
        onDuplicate = onDuplicate,
        onDurationChange = onDurationChange,
        onCopy = onCopy,
        onDragAndDropRequest = onDragAndDropRequest,
    )
}

/**
 * 動画の長さだけずっっと横に長いタイムライン。[TimeLineContainer]を親にする。
 * 横と縦に長いので、親はスクロールできるようにする必要があります。
 *
 * @param modifier [Modifier]
 * @param timeLineState タイムラインのデータ
 * @param onSeek シークがリクエストされた
 * @param timeLineScrollableAreaCoordinates このコンポーネントのサイズ
 * @param currentPositionMs 再生位置
 * @param onDragAndDropRequest [TimeLineItemComponentDragAndDropData]参照。ドラッグアンドドロップで移動先に移動してよいかを返します
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete メニューで削除を押した
 * @param onDuplicate メニューで複製を押した
 * @param onCopy メニューでコピーを押した
 * @param onDurationChange 長さ調整がリクエストされた
 */
@Composable
fun RequiredSizeTimeLine(
    modifier: Modifier = Modifier,
    timeLineState: TimeLineState,
    onSeek: (positionMs: Long) -> Unit,
    timeLineScrollableAreaCoordinates: LayoutCoordinates?,
    currentPositionMs: () -> Long,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onCopy: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Unit
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
                TimeLineLane(
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
                    currentPositionMs = currentPositionMs,
                    onEdit = onEdit,
                    onCut = onCut,
                    onDelete = onDelete,
                    onDuplicate = onDuplicate,
                    onCopy = onCopy,
                    onDurationChange = onDurationChange,
                    timeLineScrollableAreaCoordinates = timeLineScrollableAreaCoordinates,
                    magnetPositionList = timeLineState.magnetPositionList,
                    onDragAndDropRequest = { dragAndDropData ->
                        val (id, start, stop, positionMs) = dragAndDropData
                        // ドラッグアンドドロップで移動先に移動してよいか
                        // 移動元、移動先のレーン番号を取得
                        val fromLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(start.center) }.keys.firstOrNull() ?: return@TimeLineLane
                        val toLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(stop.center) }.keys.firstOrNull() ?: return@TimeLineLane

                        // 移動先の TimeLineItemData を作る
                        val request = TimeLineData.DragAndDropRequest(
                            id = id,
                            dragAndDroppedStartMs = positionMs,
                            dragAndDroppedLaneIndex = toLaneIndex
                        )

                        // 渡して処理させる
                        onDragAndDropRequest(request)
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

/**
 * タイムラインのレーンです。回転寿司。
 * Box にしてオフセットで調整しています。
 *
 * 画面外にも描画するのでなんとかしたい所存。
 *
 * @param modifier [Modifier]
 * @param laneIndex レーン番号
 * @param laneItemList タイムラインに表示するアイテムの配列[TimeLineData.Item]
 * @param currentPositionMs 現在の再生位置（赤いバーがある位置）
 * @param timeLineScrollableAreaCoordinates 横に長いタイムラインを表示しているコンポーネントの[LayoutCoordinates]
 * @param onDragAndDropRequest [TimeLineItemComponentDragAndDropData]参照。ドラッグアンドドロップで移動先に移動してよいかを返します
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete メニューで削除を押した
 * @param onDuplicate メニューで複製を押した
 * @param onCopy メニューでコピーを押した
 * @param onDurationChange 長さ調整がリクエストされた
 */
@Composable
private fun TimeLineLane(
    modifier: Modifier = Modifier,
    laneItemList: List<TimeLineData.Item>,
    laneIndex: Int,
    currentPositionMs: () -> Long,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    magnetPositionList: List<TimeLineState.MagnetPosition>,
    onDragAndDropRequest: (TimeLineItemComponentDragAndDropData) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
    onCopy: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit
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
            DefaultTimeLineItem(
                timeLineItemData = timeLineItemData,
                currentPositionMs = currentPositionMs,
                onDragAndDropRequest = onDragAndDropRequest,
                timeLineScrollableAreaCoordinates = timeLineScrollableAreaCoordinates,
                magnetPositionList = magnetPositionList,
                onEdit = { onEdit(timeLineItemData) },
                onCut = { onCut(timeLineItemData) },
                onDelete = { onDelete(timeLineItemData) },
                onDuplicate = { onDuplicate(timeLineItemData) },
                onCopy = { onCopy(timeLineItemData) },
                onDurationChange = onDurationChange
            )
        }
    }
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
 * @param onCopy コピーを押した
 * @param onDurationChange 長さ調整がリクエストされた。長さ調整つまみを離したら呼ばれる。
 */
@Composable
private fun DefaultTimeLineItem(
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
    onCopy: () -> Unit,
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
            onCopy = onCopy,
            onDuplicate = onDuplicate
        )
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
 * @param onCopy コピーを押した
 */
@Composable
private fun TimeLineItemContextMenu(
    isVisibleMenu: Boolean,
    isEnableCut: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onCopy: () -> Unit
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
            text = { Text(text = stringResource(id = R.string.timeline_context_menu_copy)) },
            onClick = {
                onCopy()
                onDismissRequest()
            },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.content_paste_24px), contentDescription = null) }
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

