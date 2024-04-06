package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.groupByLane
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/** 1 ミリ秒をどれだけの幅で表すか */
private const val MillisecondsWidthPx = 20

/** 1 ミリ秒をどれだけの幅で表すか。[dp]版 */
private val Long.msToWidthDp: Dp
    // Pixel to Dp
    @Composable
    get() = with(LocalDensity.current) { msToWidth.toDp() }

/** 1 ミリ秒をどれだけの幅で表すか。[Int]版。 */
private val Long.msToWidth: Int
    get() = (this / MillisecondsWidthPx).toInt()

/** 幅や位置は何 ミリ秒を表しているか */
private val Int.widthToMs: Long
    get() = (this * MillisecondsWidthPx).toLong()

/** 幅や位置は何 ミリ秒を表しているか */
private val Float.widthToMs: Long
    get() = (this * MillisecondsWidthPx).toLong()

/**
 * タイムラインのアイテムが移動したときに貰えるデータ
 *
 * @param id [TimeLineData.Item.id]と同じ
 * @param startRect 移動前の位置
 * @param stopRect 移動後の位置
 */
private data class TimeLineItemComponentDragAndDropData(
    val id: Long,
    val startRect: IntRect,
    val stopRect: IntRect
)

/** タイムライン */
@Composable
fun TimeLine(
    modifier: Modifier = Modifier,
    timeLineData: TimeLineData = TimeLineData(
        durationMs = 30_000,
        laneCount = 5,
        itemList = listOf(
            TimeLineData.Item(id = 1, laneIndex = 0, startMs = 0, stopMs = 10_000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 2, laneIndex = 0, startMs = 10_000, stopMs = 20_000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 3, laneIndex = 1, startMs = 1000, stopMs = 2000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 4, laneIndex = 2, startMs = 0, stopMs = 2000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 5, laneIndex = 3, startMs = 1000, stopMs = 1500, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
            TimeLineData.Item(id = 6, laneIndex = 4, startMs = 10_000, stopMs = 11_000, label = "素材", iconResId = R.drawable.ic_outline_audiotrack_24, false),
        )
    ),
    currentPositionMs: Long,
    durationMs: Long,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Boolean,
    onSeek: (positionMs: Long) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit, // TODO これ TimeLineData.Item 全部のパラメーターは要らないわ。
    onDelete: (TimeLineData.Item) -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit
) {
    // 一番遅い時間
    val maxDurationMs = remember(timeLineData) { maxOf(timeLineData.itemList.maxOfOrNull { it.stopMs } ?: 0, timeLineData.durationMs) }
    // レーンコンポーネントの位置と番号
    val timeLineLaneIndexRectMap = remember { mutableStateMapOf<Int, IntRect>() }
    // レーンとレーンのアイテムの Map
    val laneItemMap = remember(timeLineData) { timeLineData.groupByLane() }

    // はみ出しているタイムラインの LayoutCoordinates
    // タイムラインのレーンや、タイムラインのアイテムの座標を出すのに必要
    val timelineScrollableAreaCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState()),
    ) {
        // 横に長ーいタイムラインを作る
        // 画面外にはみ出すので requiredWidth
        Column(
            modifier = Modifier
                .requiredWidth(maxDurationMs.msToWidthDp)
                // タイムラインにあるアイテムの座標出すのに使う
                .onGloballyPositioned { timelineScrollableAreaCoordinates.value = it }
                // 再生位置の移動。タイムラインの棒を移動させる
                .pointerInput(Unit) {
                    detectTapGestures { onSeek(it.x.toInt().widthToMs) }
                },
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            // 時間を表示するやつ
            TimeLineTopTimeLabel(durationMs = maxDurationMs)
            HorizontalDivider()

            if (timelineScrollableAreaCoordinates.value != null) {

                // タイムラインのアイテム
                // レーンの数だけ
                laneItemMap.forEach { (laneIndex, itemList) ->
                    TimeLineLane(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .onGloballyPositioned {
                                timeLineLaneIndexRectMap[laneIndex] = timelineScrollableAreaCoordinates.value!!
                                    .localBoundingBoxOf(it)
                                    .roundToIntRect()
                            },
                        laneIndex = laneIndex,
                        laneItemList = itemList,
                        currentPositionMs = currentPositionMs,
                        onEdit = onEdit,
                        onCut = onCut,
                        onDelete = onDelete,
                        onDurationChange = onDurationChange,
                        timeLineScrollableAreaCoordinates = timelineScrollableAreaCoordinates.value!!,
                        onDragAndDropRequest = {
                            val (id, start, stop) = it
                            // ドラッグアンドドロップで移動先に移動してよいか
                            // 移動元、移動先のレーン番号を取得
                            val fromLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(start.center) }.keys.firstOrNull() ?: return@TimeLineLane false
                            val toLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(stop.center) }.keys.firstOrNull() ?: return@TimeLineLane false

                            // ドラッグアンドドロップが終わった後の位置に対応する時間を出す
                            val stopMsInDroppedPos = stop.left.widthToMs
                            // 移動先の TimeLineItemData を作る
                            val request = TimeLineData.DragAndDropRequest(
                                id = id,
                                dragAndDroppedStartMs = stopMsInDroppedPos,
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

        // タイムラインの縦の棒
        // タイムラインに重ねて使う
        if (timelineScrollableAreaCoordinates.value != null) {
            val barModifier = Modifier.height(with(LocalDensity.current) { timelineScrollableAreaCoordinates.value!!.size.height.toDp() })

            // 動画の長さ
            TimeLinePositionBar(
                modifier = barModifier,
                color = Color.Blue,
                positionMs = durationMs
            )

            // 再生位置
            TimeLinePositionBar(
                modifier = barModifier,
                color = Color.Red,
                positionMs = currentPositionMs
            )
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
 * @param onClick 押したときに呼ばれる
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete メニューで削除を押した
 * @param onDurationChange 長さ調整がリクエストされた
 */
@Composable
private fun TimeLineLane(
    modifier: Modifier = Modifier,
    laneItemList: List<TimeLineData.Item>,
    laneIndex: Int,
    currentPositionMs: Long,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    onDragAndDropRequest: (TimeLineItemComponentDragAndDropData) -> Boolean = { false },
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit,
    onDelete: (TimeLineData.Item) -> Unit,
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
            TimeLineItem(
                timeLineItemData = timeLineItemData,
                currentPositionMs = currentPositionMs,
                onDragAndDropRequest = onDragAndDropRequest,
                timeLineScrollableAreaCoordinates = timeLineScrollableAreaCoordinates,
                onEdit = { onEdit(timeLineItemData) },
                onCut = { onCut(timeLineItemData) },
                onDelete = { onDelete(timeLineItemData) },
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
 * @param onDragAndDropRequest ドラッグアンドドロップで指を離したら呼ばれます。引数は[TimeLineItemComponentDragAndDropData]参照。返り値はドラッグアンドドロップが成功したかです。移動先が空いていない等は false
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete 削除を押した
 * @param onDurationChange 長さ調整がリクエストされた。長さ調整つまみを離したら呼ばれる。
 */
@Composable
private fun TimeLineItem(
    modifier: Modifier = Modifier,
    timeLineItemData: TimeLineData.Item,
    currentPositionMs: Long,
    timeLineScrollableAreaCoordinates: LayoutCoordinates,
    onDragAndDropRequest: (TimeLineItemComponentDragAndDropData) -> Boolean = { false },
    onEdit: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit
) {
    // アイテムが移動中かどうか
    val isDragging = remember { mutableStateOf(false) }
    // アイテムの位置
    val latestGlobalRect = remember { mutableStateOf<IntRect?>(null) }
    // アイテムの移動中位置。アイテムが変化したら作り直す
    val draggingOffset = remember(timeLineItemData) { mutableStateOf(IntOffset(timeLineItemData.startMs.msToWidth, 0)) }
    // メニューを表示するか
    val isVisibleMenu = remember { mutableStateOf(false) }
    // タイムラインのアイテムの表示時間。長さ調整出来るように State で持っている。
    val durationMs = remember(timeLineItemData) { mutableLongStateOf(timeLineItemData.durationMs) }

    Surface(
        modifier = modifier
            .offset {
                // 移動中の場合はそのオフセット
                // 移動中じゃない場合は再生開始位置分オフセットでずらす
                if (isDragging.value) {
                    draggingOffset.value
                } else {
                    IntOffset(timeLineItemData.startMs.msToWidth, 0)
                }
            }
            .width(durationMs.longValue.msToWidthDp)
            .fillMaxHeight()
            .onGloballyPositioned {
                // timeLineScrollableAreaCoordinates の理由は TimeLine() コンポーネント参照
                latestGlobalRect.value = timeLineScrollableAreaCoordinates
                    .localBoundingBoxOf(it)
                    .roundToIntRect()
            }
            .pointerInput(timeLineItemData, currentPositionMs) {
                // アイテム移動のシステム
                // データが変化したら pointerInput も再起動するようにキーに入れる
                var startRect: IntRect? = null
                detectDragGestures(
                    onDragStart = {
                        // ドラッグアンドドロップ開始時。フラグを立てて開始位置を入れておく
                        isDragging.value = true
                        startRect = latestGlobalRect.value
                    },
                    onDrag = { change, dragAmount ->
                        // 移動中
                        change.consume()
                        val x = (draggingOffset.value.x + dragAmount.x).toInt()
                        val y = (draggingOffset.value.y + dragAmount.y).toInt()
                        // もし現在の位置のすぐ近くに移動させる場合は、吸い付かせる
                        draggingOffset.value = if (abs(currentPositionMs - x.widthToMs) < 100) {
                            IntOffset(currentPositionMs.msToWidth, y)
                        } else {
                            IntOffset(x, y)
                        }
                    },
                    onDragEnd = {
                        // 移動終了
                        isDragging.value = false
                        // 移動できるか判定を上のコンポーネントでやる
                        val isAccept = onDragAndDropRequest(
                            TimeLineItemComponentDragAndDropData(
                                id = timeLineItemData.id,
                                startRect = startRect ?: return@detectDragGestures,
                                stopRect = latestGlobalRect.value ?: return@detectDragGestures
                            )
                        )
                        // 出来ない場合は戻す
                        if (!isAccept) {
                            draggingOffset.value = IntOffset(timeLineItemData.startMs.msToWidth, 0)
                        }
                    }
                )
            },
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        onClick = { isVisibleMenu.value = true }
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
            if (timeLineItemData.isChangeDuration) {
                DurationChangeHandle(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    resetKey = timeLineItemData,
                    onDrag = { dragAmountX -> durationMs.longValue += dragAmountX.widthToMs },
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
        }

        // ドロップダウンメニュー
        // ボトムシートを出すとか
        TimeLineItemContextMenu(
            isVisibleMenu = isVisibleMenu.value,
            isEnableCut = currentPositionMs in timeLineItemData.timeRange,
            onDismissRequest = { isVisibleMenu.value = false },
            onEdit = onEdit,
            onCut = onCut,
            onDelete = onDelete
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
 */
@Composable
private fun TimeLineItemContextMenu(
    isVisibleMenu: Boolean,
    isEnableCut: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit
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
            text = { Text(text = stringResource(id = R.string.timeline_context_menu_delete)) },
            onClick = {
                onDelete()
                onDismissRequest()
            },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null) }
        )
    }
}

/** タイムラインの一番上に表示する時間 */
@Composable
private fun TimeLineTopTimeLabel(
    modifier: Modifier = Modifier,
    durationMs: Long,
    stepMs: Long = 10_000 // 10 秒間隔
) {
    val simpleDateFormat = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }

    val labelList = remember(durationMs, stepMs) {
        // 最後まで
        ((0 until durationMs step stepMs) + durationMs).toList()
    }

    Box(modifier = modifier) {
        labelList.forEach { timeMs ->
            val textMeasure = rememberTextMeasurer()
            val timeText = simpleDateFormat.format(timeMs)

            Text(
                modifier = Modifier
                    .offset { IntOffset(-textMeasure.measure(timeText).size.width / 2, 0) }
                    .offset { IntOffset(timeMs.msToWidth, 0) },
                text = timeText
            )
        }
    }
}

/**
 * タイムラインで現在位置とか、最後の位置を表示するためのバー
 *
 * @param modifier [Modifier]
 * @param positionMs バーを表示したい位置
 */
@Composable
private fun TimeLinePositionBar(
    modifier: Modifier = Modifier,
    color: Color,
    positionMs: Long
) {
    Box(
        modifier = modifier
            .width(2.dp)
            .offset { IntOffset(positionMs.msToWidth, 0) }
            .background(color)
    )
}

/**
 * 長さ調整用のつまみ
 *
 * @param modifier [Modifier]
 * @param resetKey [Modifier.pointerInput]のリセット用
 * @param onDrag つまみ移動中に呼ばれる
 * @param onDragEnd つまみ移動が終わったら（指を離したら）呼ばれる
 */
@Composable
private fun DurationChangeHandle(
    modifier: Modifier = Modifier,
    resetKey: Any,
    onDrag: (dragAmountX: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Row(
        modifier = modifier.pointerInput(resetKey) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                },
                onDragEnd = onDragEnd
            )
        },
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
