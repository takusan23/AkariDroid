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
import io.github.takusan23.akaridroid.ui.component.data.TimeLineDragAndDropData
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
        TimeLineItemData(id = 1, laneIndex = 0, startMs = 0, stopMs = 10_000),
        TimeLineItemData(id = 2, laneIndex = 0, startMs = 10_000, stopMs = 20_000),
        TimeLineItemData(id = 3, laneIndex = 1, startMs = 1000, stopMs = 2000),
        TimeLineItemData(id = 4, laneIndex = 2, startMs = 0, stopMs = 2000),
        TimeLineItemData(id = 5, laneIndex = 3, startMs = 1000, stopMs = 1500),
        TimeLineItemData(id = 6, laneIndex = 4, startMs = 10_000, stopMs = 11_000),
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
                        onDragAndDropRequest = {
                            val (_, start, stop) = it
                            // ドラッグアンドドロップで移動先に移動してよいか
                            // 移動元、移動先のレーン番号を取得
                            val fromLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(start.center) }.keys.firstOrNull() ?: return@TimeLineSushiLane false
                            val toLaneIndex = timeLineLaneIndexRectMap.filter { it.value.contains(stop.center) }.keys.firstOrNull() ?: return@TimeLineSushiLane false

                            // ドラッグアンドドロップが終わった後の位置に対応する時間を出す
                            val stopMsInDroppedPos = stop.left.widthToMs
                            // 移動先の TimeLineItemData を作る
                            val afterTarget = it.target.copy(
                                laneIndex = toLaneIndex,
                                startMs = stopMsInDroppedPos,
                                stopMs = stopMsInDroppedPos + it.target.durationMs
                            )

                            // 移動先のレーンに空きがあること
                            // 同一レーンの移動の場合は自分自身も消す（時間調整できなくなる）
                            val isAcceptable = sushiItemList.value
                                .filter { laneItem -> laneItem.laneIndex == afterTarget.laneIndex }
                                .filter { laneItem -> laneItem.id != afterTarget.id }
                                .all { laneItem ->
                                    // 空きがあること
                                    val hasFreeSpace = afterTarget.startMs !in laneItem.timeRange && afterTarget.stopMs !in laneItem.timeRange
                                    // 移動先に重なる形で自分より小さいアイテムが居ないこと
                                    val hasNotInclude = laneItem.startMs !in afterTarget.timeRange && laneItem.stopMs !in afterTarget.timeRange
                                    hasFreeSpace && hasNotInclude
                                }

                            sushiItemList.value = if (isAcceptable) {
                                // データ変更
                                sushiItemList.value.filter { lineItem -> lineItem.id != afterTarget.id } + afterTarget
                            } else {
                                sushiItemList.value.toList()
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
    onDragAndDropRequest: (TimeLineDragAndDropData) -> Boolean = { false },
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

/**
 * タイムラインに表示するアイテム
 *
 * @param modifier [Modifier]
 * @param onClick 押したときに呼ばれる
 * @param timeLineItemData タイムラインのデータ[TimeLineItemData]
 * @param onDragAndDropRequest ドラッグアンドドロップで指を離したら呼ばれます。引数は[TimeLineDragAndDropData]参照。返り値はドラッグアンドドロップが成功したかです。移動先が空いていない等は false
 */
@Composable
private fun TimeLineSushiItem(
    modifier: Modifier = Modifier,
    timeLineItemData: TimeLineItemData,
    onDragAndDropRequest: (TimeLineDragAndDropData) -> Boolean = { false },
    onClick: () -> Unit
) {
    // アイテムが移動中かどうか
    val isDragging = remember { mutableStateOf(false) }
    // アイテムの位置
    val latestGlobalRect = remember { mutableStateOf<IntRect?>(null) }
    // アイテムの移動中位置。アイテムが変化したら作り直す
    val draggingOffset = remember(timeLineItemData) { mutableStateOf(IntOffset(timeLineItemData.startMs.msToWidth, 0)) }

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
            .width((timeLineItemData.stopMs - timeLineItemData.startMs).msToWidthDp)
            .fillMaxHeight()
            .onGloballyPositioned {
                latestGlobalRect.value = it
                    .boundsInWindow()
                    .roundToIntRect()
            }
            .pointerInput(timeLineItemData) {
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
                        draggingOffset.value = IntOffset(
                            x = (draggingOffset.value.x + dragAmount.x).toInt(),
                            y = (draggingOffset.value.y + dragAmount.y).toInt()
                        )
                    },
                    onDragEnd = {
                        // 移動終了
                        isDragging.value = false
                        // 移動できるか判定を上のコンポーネントでやる
                        val isAccept = onDragAndDropRequest(
                            TimeLineDragAndDropData(
                                target = timeLineItemData,
                                start = startRect ?: return@detectDragGestures,
                                stop = latestGlobalRect.value ?: return@detectDragGestures
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
