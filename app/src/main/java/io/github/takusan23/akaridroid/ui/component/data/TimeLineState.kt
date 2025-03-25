package io.github.takusan23.akaridroid.ui.component.data

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val scrollState = rememberScrollState()
    val state = remember { DefaultTimeLineState(scope, scrollState) }
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

    /** タイムラインの横スクロール[] */
    val horizontalScroll: ScrollState

    /** タイムラインのスクロールするコンポーネントの幅 */
    var timeLineParentWidth: Int

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
class DefaultTimeLineState(
    scope: CoroutineScope,
    override val horizontalScroll: ScrollState
) : TimeLineState {
    /** ViewModel が作ってる [TimeLineData] */
    var timeLineData by mutableStateOf<TimeLineData?>(null)

    /** タイムラインの拡大縮小 */
    override var timeLineMillisecondsWidthPx by mutableStateOf(TimeLineMillisecondsWidthPx())

    /** タイムラインのスクロールするコンポーネントの幅 */
    override var timeLineParentWidth by mutableIntStateOf(0)

    override var visibleTimeLineItemMap by mutableStateOf(emptyMap<Int, List<TimeLineData.Item>>())
        private set

    override var maxDurationMs: Long by mutableLongStateOf(0)
        private set

    override var magnetPositionList by mutableStateOf(emptyList<TimeLineState.MagnetPosition>())
        private set

    init {
        // Flow にする
        val timeLineDataFlow = snapshotFlow { timeLineData }
        val msWidthPxFlow = snapshotFlow { timeLineMillisecondsWidthPx }

        // スクロールに対応して、今表示しているタイムラインの表示領域を計算する
        val visibleTimeLineWidthRange = combine(
            snapshotFlow { horizontalScroll.value },
            snapshotFlow { timeLineParentWidth },
            { startScroll, timeLineParent -> startScroll..(startScroll + timeLineParent) }
        )

        scope.launch {
            // 別スレッドでやるので
            withContext(Dispatchers.Default) {

                // スクロール位置の変更やタイムラインの追加で
                combine(
                    timeLineDataFlow,
                    visibleTimeLineWidthRange,
                    msWidthPxFlow,
                    ::Triple
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