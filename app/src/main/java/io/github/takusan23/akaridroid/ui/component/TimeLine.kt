package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.data.TimeLineItemData
import java.text.SimpleDateFormat
import java.util.Locale

/** ミリ秒を秒にする */
private val Long.second: Int
    get() = (this / 1000).toInt()

/** 1 ミリ秒をどれだけの幅で表すか。[dp]版 */
private val Long.msToWidthDp: Dp
    // Pixel to Dp
    @Composable
    get() = with(LocalDensity.current) { msToWidth.toDp() }

/** 1 ミリ秒をどれだけの幅で表すか。[Int]版。 */
private val Long.msToWidth: Int
    get() = (this / 20).toInt()

/** タイムライン */
@Composable
fun TimeLine(
    modifier: Modifier = Modifier,
    durationMs: Long = 30_000,
    itemList: List<TimeLineItemData> = listOf(
        TimeLineItemData(0, 0, 10_000),
        TimeLineItemData(0, 10_000, 20_000),
        TimeLineItemData(1, 1000, 2000),
        TimeLineItemData(2, 0, 2000),
        TimeLineItemData(3, 1000, 1500),
        TimeLineItemData(4, 10_000, 11_000),
    )
) {
    // 一番遅い時間
    val maxWidth = remember(itemList, durationMs) {
        maxOf(itemList.maxBy { it.stopMs }.stopMs, durationMs)
    }

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

            // タイムラインのアイテム
            // レーンの数だけ
            itemList
                .groupBy { it.laneIndex }
                .forEach { (laneIndex, itemList) ->
                    TimeLineSushiLane(
                        modifier = Modifier.height(50.dp),
                        laneIndex = laneIndex,
                        laneItemList = itemList,
                        onClick = {}
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
 * @param onClick 押したときに呼ばれる
 */
@Composable
private fun TimeLineSushiLane(
    modifier: Modifier = Modifier,
    laneItemList: List<TimeLineItemData>,
    laneIndex: Int,
    onClick: (TimeLineItemData) -> Unit
) {
    Box(modifier = modifier) {

        Text(
            text = (laneIndex + 1).toString(),
            modifier = Modifier
                .padding(start = 10.dp)
                .alpha(0.5f)
                .align(Alignment.Center),
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary
        )

        laneItemList.forEach { timeLineItemData ->
            TimeLineItem(
                timeLineItemData = timeLineItemData,
                onClick = { onClick(timeLineItemData) }
            )
        }
    }
}

/** タイムラインに表示するアイテム */
@Composable
private fun TimeLineItem(
    modifier: Modifier = Modifier,
    timeLineItemData: TimeLineItemData,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .offset { IntOffset(timeLineItemData.startMs.msToWidth, 0) }
            .width((timeLineItemData.stopMs - timeLineItemData.startMs).msToWidthDp)
            .fillMaxHeight(),
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
