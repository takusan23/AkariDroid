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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.TimeLineMillisecondsWidthPx
import io.github.takusan23.akaridroid.ui.component.data.TimeLineState
import java.text.SimpleDateFormat
import java.util.Locale

/** タイムラインの拡大、縮小を CompositionLocal で提供する、バケツリレーを回避する */
val LocalTimeLineMillisecondsWidthPx = compositionLocalOf { TimeLineMillisecondsWidthPx() }

/**
 * タイムラインのアイテム移動中に、隣接するアイテムの隣に置きたい場合に、磁石のように勝手にくっつく機能がある。
 * 磁石モード。これのしきい値
 */
const val MAGNET_THRESHOLD_MOVE = 50

/** 磁石モード。長さ調整版。[MAGNET_THRESHOLD_MOVE]よりも大きくしないとズレて使いにくかった */
const val MAGNET_THRESHOLD_DURATION_CHANGE = 200

/**
 * タイムラインのアイテムが移動したときに貰えるデータ
 * [startRect]、[stopRect]はレーンの判定だけに使ってください。時間をこの[IntRect]から出すと多分正確じゃない（丸められる）
 *
 * @param id [TimeLineData.Item.id]と同じ
 * @param startRect 移動前の位置
 * @param stopRect 移動後の位置
 * @param positionMs 移動後の再生開始位置
 */
data class TimeLineItemComponentDragAndDropData(
    val id: Long,
    val startRect: IntRect,
    val stopRect: IntRect,
    val positionMs: Long
)

/** デフォルトのタイムライン */
@Composable
fun DefaultTimeLine(
    modifier: Modifier = Modifier,
    timeLineState: TimeLineState,
    currentPositionMs: () -> Long,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Boolean,
    onSeek: (positionMs: Long) -> Unit,
    onEdit: (TimeLineData.Item) -> Unit,
    onCut: (TimeLineData.Item) -> Unit, // TODO これ TimeLineData.Item 全部のパラメーターは要らないわ。
    onDelete: (TimeLineData.Item) -> Unit,
    onDuplicate: (TimeLineData.Item) -> Unit,
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
        onDragAndDropRequest = onDragAndDropRequest,
    )
}

/**
 * 動画の長さだけずっっと横に長いタイムライン。[TimeLineContainer]を親にする。
 * 横と縦に長いので、親はスクロールできるようにする必要があります。
 *
 * @param modifier [Modifier]
 * @param timeLineData タイムラインのデータ
 * @param onSeek シークがリクエストされた
 * @param timeLineScrollableAreaCoordinates このコンポーネントのサイズ
 * @param currentPositionMs 再生位置
 * @param onDragAndDropRequest [TimeLineItemComponentDragAndDropData]参照。ドラッグアンドドロップで移動先に移動してよいかを返します
 * @param onEdit メニューで値の編集を押した
 * @param onCut メニューで分割を押した
 * @param onDelete メニューで削除を押した
 * @param onDuplicate メニューで複製を押した
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
    onDurationChange: (TimeLineData.DurationChangeRequest) -> Unit,
    onDragAndDropRequest: (request: TimeLineData.DragAndDropRequest) -> Boolean
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
                onDurationChange = onDurationChange
            )
        }
    }
}

/** タイムラインの一番上に表示する時間 */
@Composable
fun TimeLineTopTimeLabel(
    modifier: Modifier = Modifier,
    durationMs: Long,
    stepMs: Long = 10_000 // 10 秒間隔
) {
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current
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
                    .offset { IntOffset(with(msWidthPx) { timeMs.msToWidth }, 0) },
                text = timeText
            )
        }
    }
}
