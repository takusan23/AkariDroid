package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.ui.component.data.TimeLineMillisecondsWidthPx
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

/** [DefaultTimeLine]と[MultiSelectTimeLine]の共通部分 */
@Composable
fun TimeLineContainer(
    modifier: Modifier = Modifier,
    timeLineMillisecondsWidthPx: TimeLineMillisecondsWidthPx,
    verticalScroll: ScrollState = rememberScrollState(),
    horizontalScroll: ScrollState = rememberScrollState(),
    durationMs: () -> Long,
    currentPositionMs: () -> Long,
    onScrollContainerSizeChange: (IntSize) -> Unit,
    content: @Composable () -> Unit
) {
    // millisecondsWidthPx を LocalTimeLineMillisecondsWidthPx で提供する
    CompositionLocalProvider(value = LocalTimeLineMillisecondsWidthPx provides timeLineMillisecondsWidthPx) {
        Box(
            modifier = modifier
                .onSizeChanged(onScrollContainerSizeChange)
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll),
        ) {

            // 横に長ーいタイムラインを作る
            content()

            // タイムラインの縦の棒。タイムラインに重ねて使う
            // matchParentSize で親 Box の大きさに合わせる
            OverlayTimeLineComponents(
                modifier = Modifier.matchParentSize(),
                durationMs = durationMs,
                currentPositionMs = currentPositionMs
            )
        }
    }
}

/**
 * タイムラインの縦の棒。タイムラインに重ねるコンポーネント
 *
 * @param modifier [Modifier]
 * @param durationMs 動画時間
 * @param currentPositionMs 再生位置
 */
@Composable
private fun OverlayTimeLineComponents(
    modifier: Modifier = Modifier,
    durationMs: () -> Long,
    currentPositionMs: () -> Long
) {
    Box(modifier) {
        // 動画の長さ
        TimeLinePositionBar(
            modifier = Modifier.fillMaxHeight(),
            color = Color.Blue,
            positionMs = durationMs
        )

        // 再生位置
        TimeLinePositionBar(
            modifier = Modifier.fillMaxHeight(),
            color = Color.Red,
            positionMs = currentPositionMs
        )
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
    positionMs: () -> Long
) {
    val msWidthPx = LocalTimeLineMillisecondsWidthPx.current

    Box(
        modifier = modifier
            .width(2.dp)
            .offset { IntOffset(with(msWidthPx) { positionMs().msToWidth }, 0) }
            .background(color)
    )
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
