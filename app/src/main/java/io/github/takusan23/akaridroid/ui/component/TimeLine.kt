package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.data.TimeLineItemData
import io.github.takusan23.akaridroid.ui.component.data.durationMs
import io.github.takusan23.akaridroid.ui.component.data.timeRange
import java.text.SimpleDateFormat
import java.util.Locale

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

/** タイムライン */
@Composable
fun TimeLine(
    modifier: Modifier = Modifier,
    durationMs: Long = 30_000,
    itemList: List<TimeLineItemData> = listOf(
        TimeLineItemData(laneIndex = 0, startMs = 0, stopMs = 10_000),
        TimeLineItemData(laneIndex = 0, startMs = 10_000, stopMs = 20_000),
        TimeLineItemData(laneIndex = 1, startMs = 1000, stopMs = 2000),
        TimeLineItemData(laneIndex = 2, startMs = 0, stopMs = 2000),
        TimeLineItemData(laneIndex = 3, startMs = 1000, stopMs = 1500),
        TimeLineItemData(laneIndex = 4, startMs = 10_000, stopMs = 11_000),
    ),
    onClick: (TimeLineItemData) -> Unit = {}
) {
    val sushiItemList = remember { mutableStateOf(itemList) }
    // 一番遅い時間
    val maxWidth = remember(itemList, durationMs) { maxOf(itemList.maxBy { it.stopMs }.stopMs, durationMs) }
    // レーンコンポーネントの位置と番号
    val timeLineLaneIndexRectMap = remember { mutableStateMapOf<Int, IntRect>() }

    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState()),
    ) {
        Column(
            // 画面外にはみ出すので requiredWidth
            modifier = modifier.requiredWidth(maxWidth.msToWidthDp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            // 時間を表示するやつ
            TimeLineTopTimeLabel(durationMs = durationMs)
            Divider()

            // タイムラインのアイテム
            // レーンの数だけ
            sushiItemList
                .value
                .groupBy { it.laneIndex }
                .forEach { (laneIndex, itemList) ->

                    TimeLineSushiLane(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .onGloballyPositioned {
                                timeLineLaneIndexRectMap[laneIndex] = it
                                    .boundsInWindow()
                                    .roundToIntRect()
                            },
                        laneIndex = laneIndex,
                        laneItemList = itemList,
                        onClick = onClick,
                        onDragAndDropRequest = { target, start, stop ->
                            // ドラッグアンドドロップで移動先に移動してよいか
                            // 移動元、移動先のレーン番号を取得
                            val fromLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(start.center) }.keys.firstOrNull() ?: return@TimeLineSushiLane false
                            val toLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(stop.center) }.keys.firstOrNull() ?: return@TimeLineSushiLane false

                            // 移動先のレーンに空きがあること
                            val isAcceptable = sushiItemList.value
                                .filter { it.laneIndex == toLaneIndex }
                                .all { laneItem ->
                                    // 空きがあること
                                    val hasFreeSpace = target.startMs !in laneItem.timeRange && target.stopMs !in laneItem.timeRange
                                    // 移動先に自分より小さいアイテムが居ないこと
                                    val hasNotInclude = laneItem.startMs !in target.timeRange && laneItem.stopMs !in target.timeRange
                                    hasFreeSpace && hasNotInclude
                                }

                            if (isAcceptable) {
                                // データ変更
                                sushiItemList.value = sushiItemList.value.filter { it.id != target.id } + target.copy(laneIndex = toLaneIndex)
                            }

                            // 受け入れられることを伝える
                            isAcceptable
                        }
                    )
                    Divider()
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
 * @param laneItemList タイムラインに表示するアイテム[TimeLineItemData]
 * @param onDragAndDropRequest [TimeLineSushiItem]参照。ドラッグアンドドロップで移動先に移動してよいかを返します
 * @param onClick 押したときに呼ばれる
 */
@Composable
private fun TimeLineSushiLane(
    modifier: Modifier = Modifier,
    laneItemList: List<TimeLineItemData>,
    laneIndex: Int,
    onDragAndDropRequest: (target: TimeLineItemData, start: IntRect, stop: IntRect) -> Boolean = { _, _, _ -> false },
    onClick: (TimeLineItemData) -> Unit
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
            TimeLineSushiItem(
                timeLineItemData = timeLineItemData,
                onDragAndDropRequest = onDragAndDropRequest,
                onClick = { onClick(timeLineItemData) }
            )
        }
    }
}

/** タイムラインに表示するアイテム */
@Composable
private fun TimeLineSushiItem(
    modifier: Modifier = Modifier,
    timeLineItemData: TimeLineItemData,
    onDragAndDropRequest: (target: TimeLineItemData, start: IntRect, stop: IntRect) -> Boolean = { _, _, _ -> false },
    onClick: () -> Unit
) {
    // アイテムの移動に必要
    val isDragging = remember { mutableStateOf(false) }
    val latestGlobalRect = remember { mutableStateOf<IntRect?>(null) }
    val draggingOffset = remember { mutableStateOf(IntOffset(timeLineItemData.startMs.msToWidth, 0)) }

    Surface(
        modifier = modifier
            .offset {
                if (isDragging.value) {
                    draggingOffset.value
                } else {
                    IntOffset(timeLineItemData.startMs.msToWidth, 0)
                }
            }
            .width((timeLineItemData.stopMs - timeLineItemData.startMs).msToWidthDp)
            .fillMaxHeight()
            .onGloballyPositioned {
                latestGlobalRect.value = it
                    .boundsInWindow()
                    .roundToIntRect()
            }
            .pointerInput(Unit) {
                var startRect: IntRect? = null

                detectDragGestures(
                    onDragStart = {
                        isDragging.value = true
                        startRect = latestGlobalRect.value
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggingOffset.value = IntOffset(
                            x = (draggingOffset.value.x + dragAmount.x).toInt(),
                            y = (draggingOffset.value.y + dragAmount.y).toInt()
                        )
                    },
                    onDragEnd = {
                        isDragging.value = false
                        // ドラッグアンドドロップが終わった後の位置に対応する時間を出す
                        val stopMsInDroppedPos = draggingOffset.value.x.widthToMs

                        // 移動できるか判定を上のコンポーネントでやる
                        val isAccept = onDragAndDropRequest(
                            // 移動対象は copy して移動先に書き換えておく
                            timeLineItemData.copy(
                                startMs = stopMsInDroppedPos,
                                stopMs = stopMsInDroppedPos + timeLineItemData.durationMs
                            ),
                            startRect ?: return@detectDragGestures,
                            latestGlobalRect.value ?: return@detectDragGestures
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
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_audiotrack_24),
                contentDescription = null
            )
            Text(
                text = "素材 $timeLineItemData",
                maxLines = 1
            )
        }
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
            Text(
                modifier = Modifier
                    .offset { IntOffset(timeMs.msToWidth, 0) },
                text = simpleDateFormat.format(timeMs)
            )
        }
    }
}
