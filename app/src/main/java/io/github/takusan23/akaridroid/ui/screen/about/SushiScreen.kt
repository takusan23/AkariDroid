package io.github.takusan23.akaridroid.ui.screen.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData
import io.github.takusan23.akaridroid.ui.component.data.rememberTimeLineState
import io.github.takusan23.akaridroid.ui.component.timeline.DefaultTimeLine
import io.github.takusan23.akaridroid.ui.component.timeline.TimeLineContainer

private const val SUSHI_EMOJI = "\uD83C\uDF63"

/** タイムラインが正しく動作しているかのテスト用画面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSushiScreen(onBack: () -> Unit) {
    val currentPositionMs = remember { mutableLongStateOf(0) }
    val editItem = remember { mutableStateOf<TimeLineData.Item?>(null) }

    val sushiData = remember {
        mutableStateOf(
            TimeLineData(
                durationMs = 60_000,
                laneCount = 5,
                itemList = listOf(
                    TimeLineData.Item(id = 1, laneIndex = 0, startMs = 0, stopMs = 10_000, label = SUSHI_EMOJI.repeat(3), iconResId = R.drawable.ic_outline_audiotrack_24, true),
                    TimeLineData.Item(id = 2, laneIndex = 0, startMs = 10_000, stopMs = 20_000, label = SUSHI_EMOJI.repeat(3), iconResId = R.drawable.ic_outline_audiotrack_24, true),
                    TimeLineData.Item(id = 3, laneIndex = 1, startMs = 1000, stopMs = 2000, label = SUSHI_EMOJI, iconResId = R.drawable.ic_outline_audiotrack_24, true),
                    TimeLineData.Item(id = 4, laneIndex = 2, startMs = 0, stopMs = 2000, label = SUSHI_EMOJI, iconResId = R.drawable.ic_outline_audiotrack_24, false),
                    TimeLineData.Item(id = 5, laneIndex = 3, startMs = 1000, stopMs = 1500, label = SUSHI_EMOJI, iconResId = R.drawable.ic_outline_audiotrack_24, false),
                    TimeLineData.Item(id = 6, laneIndex = 4, startMs = 10_000, stopMs = 11_000, label = SUSHI_EMOJI, iconResId = R.drawable.ic_outline_audiotrack_24, false),
                )
            )
        )
    }

    // タイムラインの状態
    val timeLineState = rememberTimeLineState(
        timeLineData = sushiData.value,
        msWidthPx = 20
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "てすと") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24px), contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TimeLineContainer(
                modifier = Modifier,
                timeLineMillisecondsWidthPx = timeLineState.timeLineMillisecondsWidthPx,
                verticalScroll = rememberScrollState(),
                horizontalScroll = timeLineState.horizontalScroll,
                durationMs = { 60_000 },
                currentPositionMs = { currentPositionMs.longValue },
                onScrollContainerSizeChange = { timeLineState.timeLineParentWidth = it.width }
            ) {

                DefaultTimeLine(
                    modifier = Modifier.weight(1f),
                    timeLineState = timeLineState,
                    currentPositionMs = { currentPositionMs.longValue },
                    onSeek = { positionMs -> currentPositionMs.longValue = positionMs },
                    onDragAndDropRequest = { request ->
                        // 位置更新のみ、入るかの判定はしていない。
                        sushiData.value = sushiData.value.copy(
                            itemList = sushiData.value.itemList.map { sushi ->
                                if (sushi.id == request.id) {
                                    sushi.copy(
                                        laneIndex = request.dragAndDroppedLaneIndex,
                                        startMs = request.dragAndDroppedStartMs,
                                        stopMs = request.dragAndDroppedStartMs + sushi.durationMs
                                    )
                                } else sushi
                            }
                        )
                        true
                    },
                    onCut = { cutItem ->
                        // 分割する前に、シーク位置が重なっているか
                        if (currentPositionMs.longValue !in cutItem.timeRange) return@DefaultTimeLine
                        // 分割するので2つ作る
                        val id = cutItem.id
                        // ID 被らんように
                        val a = cutItem.copy(id = id * 100, stopMs = currentPositionMs.longValue)
                        val b = cutItem.copy(id = id * 1000, startMs = currentPositionMs.longValue)
                        // 元のアイテムは消して、入れる
                        sushiData.value = sushiData.value.copy(
                            itemList = sushiData.value.itemList.filter { it.id != id } + listOf(a, b)
                        )
                    },
                    onEdit = {
                        editItem.value = it
                    },
                    onDelete = { deleteItem ->
                        sushiData.value = sushiData.value.copy(
                            itemList = sushiData.value.itemList.filter { it.id != deleteItem.id }
                        )
                    },
                    onDuplicate = {
                        // ID 被らんように
                        sushiData.value = sushiData.value.copy(
                            itemList = sushiData.value.itemList + it.copy(id = it.id * 2)
                        )
                    },
                    onDurationChange = { request ->
                        // 長さ調整
                        val (id, newDurationMs) = request
                        sushiData.value = sushiData.value.copy(
                            itemList = sushiData.value.itemList.map { item ->
                                if (item.id == id) {
                                    item.copy(stopMs = item.startMs + newDurationMs)
                                } else {
                                    item
                                }
                            }
                        )
                    }
                )
            }
            Text(
                modifier = Modifier.padding(10.dp),
                text = editItem.value.toString()
            )
        }
    }
}