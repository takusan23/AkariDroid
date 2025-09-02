package io.github.takusan23.akaridroid.ui.component.data

import androidx.compose.foundation.gestures.rememberScrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * タイムラインの状態を作る
 *
 * @param timeLineData タイムラインに表示するアイテム
 * @param msWidthPx タイムラインの拡大縮小
 */
@Composable
fun rememberTimeLineState(
    timeLineData: TimeLineData,
    msWidthPx: Int,
): TimeLineState {
    val scope = rememberCoroutineScope()
    val state = remember { DefaultTimeLineState(scope) }
    val timeLineMillisecondsWidthPx = remember(msWidthPx) { TimeLineMillisecondsWidthPx(msWidthPx) }

    state.timeLineData = timeLineData
    state.timeLineMillisecondsWidthPx = timeLineMillisecondsWidthPx

    return state
}

/** 余計なものを公開したくないので interface 切った。 */
interface TimeLineState {

    /**
     * レーンとレーンのアイテム配列の Map
     * 表示するべきアイテムのみが配列に入ります（画面外のアイテムは表示しないようにすることでメインスレッド軽量化）
     */
    val visibleTimeLineItemMap: Map<Int, List<TimeLineData.Item>>

    /** タイムラインの中で一番遅い時間 */
    val maxDurationMs: Long

    /** 磁石みたいにくっつく機能があるので、その位置 */
    val magnetPositionList: List<MagnetPosition>

    /** タイムラインの拡大縮小 */
    var timeLineMillisecondsWidthPx: TimeLineMillisecondsWidthPx

    /**
     * タイムラインの親 [io.github.takusan23.akaridroid.ui.component.timeline.TimeLineContainer] につける Modifier。
     * サイズ測定や縦横斜め方向のスクロールができる Modifier です。
     */
    @get:Composable
    val timeLineContainerModifier: Modifier

    /**
     * 磁石モードのくっつく位置と ID
     *
     * @param id [TimeLineData.Item.id]と同じ
     * @param positionMs くっつける位置
     */
    data class MagnetPosition(
        val id: Long,
        val positionMs: Long
    )
}

/** [TimeLineState]のデフォルト実装 */
class DefaultTimeLineState(scope: CoroutineScope) : TimeLineState {
    /** ViewModel が作ってる [TimeLineData] */
    var timeLineData by mutableStateOf<TimeLineData?>(null)

    /** タイムラインの拡大縮小 */
    override var timeLineMillisecondsWidthPx by mutableStateOf(TimeLineMillisecondsWidthPx())

    override var visibleTimeLineItemMap by mutableStateOf(emptyMap<Int, List<TimeLineData.Item>>())
        private set

    override var maxDurationMs: Long by mutableLongStateOf(0)
        private set

    override var magnetPositionList by mutableStateOf(emptyList<TimeLineState.MagnetPosition>())
        private set

    /** タイムラインのスクロール位置 */
    private var scrollOffset by mutableStateOf(Offset.Zero)

    /** タイムラインのスクロールできるサイズ */
    private var size by mutableStateOf(IntSize.Zero)

    /** タイムラインの親のサイズ（スクロールのサイズではない） */
    private var timeLineParentSize by mutableStateOf(IntSize.Zero)

    override val timeLineContainerModifier: Modifier
        @Composable
        get() = Modifier
            .onSizeChanged {
                timeLineParentSize = it
            }
            .clipToBounds() // はみ出さない
            .scrollable2D(state = rememberScrollable2DState { delta ->
                // これをしないと見えないスクロール（スクロールしても UI がなかなか反映されない）が起きる
                val newX = (scrollOffset.x + delta.x).toInt().coerceIn(-size.width..0)
                val newY = (scrollOffset.y + delta.y).toInt().coerceIn(-size.height..0)
                scrollOffset = Offset(newX.toFloat(), newY.toFloat())
                // TODO 今回は面倒なのでネストスクロールを考慮していません。
                // TODO 本来は利用した分だけ return するべきです
                delta
            })
            .layout { measurable, constraints ->
                // ここを infinity にすると左端に寄ってくれる
                val childConstraints = constraints.copy(
                    maxHeight = Constraints.Infinity,
                    maxWidth = Constraints.Infinity,
                )
                // この辺は全部 Scroll.kt のパクリ
                val placeable = measurable.measure(childConstraints)
                val width = placeable.width.coerceAtMost(constraints.maxWidth)
                val height = placeable.height.coerceAtMost(constraints.maxHeight)
                val scrollHeight = placeable.height - height
                val scrollWidth = placeable.width - width
                size = IntSize(scrollWidth, scrollHeight)
                layout(width, height) {
                    val scrollX = scrollOffset.x.toInt().coerceIn(-scrollWidth..0)
                    val scrollY = scrollOffset.y.toInt().coerceIn(-scrollHeight..0)
                    val xOffset = scrollX
                    val yOffset = scrollY
                    withMotionFrameOfReferencePlacement {
                        placeable.placeRelativeWithLayer(xOffset, yOffset)
                    }
                }
            }

    init {
        // Flow にする
        val timeLineDataFlow = snapshotFlow { timeLineData }
        val msWidthPxFlow = snapshotFlow { timeLineMillisecondsWidthPx }

        // スクロールに対応して、今表示しているタイムラインの表示領域を計算する
        val visibleTimeLineWidthRange = combine(
            flow = snapshotFlow { abs(scrollOffset.x).toInt() }, // 右にスクロール来るたびにマイナスになる、が、時間が進むに連れ増えていってほしいので abs
            flow2 = snapshotFlow { timeLineParentSize.width },
            transform = { startScroll, timeLineParent -> startScroll..(startScroll + timeLineParent) }
        )

        scope.launch {
            // 別スレッドでやるので
            withContext(Dispatchers.Default) {

                // スクロール位置の変更やタイムラインの追加で
                combine(
                    flow = timeLineDataFlow,
                    flow2 = visibleTimeLineWidthRange,
                    flow3 = msWidthPxFlow,
                    transform = ::Triple
                ).collectLatest { (timeline, widthRange, widthPx) ->
                    timeline ?: return@collectLatest

                    // 最大値
                    maxDurationMs = maxOf(timeline.itemList.maxOfOrNull { it.stopMs } ?: 0, timeline.durationMs)

                    // 磁石モード用に、くっつく位置。最初と最後
                    // +1 は、例えば2秒で終わった後に2秒に始めると、重なってしまうため
                    magnetPositionList = timeline.itemList
                        .map { item ->
                            listOf(
                                TimeLineState.MagnetPosition(item.id, item.startMs),
                                TimeLineState.MagnetPosition(item.id, item.stopMs + 1)
                            )
                        }
                        .flatten()

                    // 画面内に見えているものだけ（スクロールで画面外にあるものは描画しない）
                    // 別スレッドなので、量が多いとスクロールはスムーズだけど表示は遅延する。
                    visibleTimeLineItemMap = (0 until timeline.laneCount).associateWith { layer ->
                        timeline.itemList.filter { timeLineItem ->
                            // タイムラインのアイテムの最初 or 最後 が画面内に入っていれば
                            val offsetX = with(widthPx) { timeLineItem.startMs.msToWidth }
                            val width = with(widthPx) { timeLineItem.durationMs.msToWidth }
                            val timeLineItemWidthRange = offsetX..(offsetX + width)

                            // 最初と最後のどっちかが含まれている
                            val visibleStartOrEnd = timeLineItemWidthRange.first in widthRange || timeLineItemWidthRange.last in widthRange
                            // 画面いっぱいにタイムラインの要素が表示されている場合
                            val visibleItemOver = widthRange.first in timeLineItemWidthRange || widthRange.last in timeLineItemWidthRange
                            timeLineItem.laneIndex == layer && (visibleStartOrEnd || visibleItemOver)
                        }
                    }
                }
            }
        }
    }
}