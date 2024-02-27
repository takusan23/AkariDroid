package io.github.takusan23.akaridroid.ui.screen.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.TimeLine
import io.github.takusan23.akaridroid.ui.component.data.TimeLineData

private const val SUSHI_EMOJI = "\uD83C\uDF63"

/** タイムラインが正しく動作しているかのテスト用画面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSushiScreen(onBack: () -> Unit) {
    val currentPosition = remember { mutableLongStateOf(0) }
    val sushiData = remember {
        mutableStateOf(
            TimeLineData(
                durationMs = 60_000,
                laneCount = 5,
                itemList = listOf(
                    TimeLineData.Item(id = 1, laneIndex = 0, startMs = 0, stopMs = 10_000, label = SUSHI_EMOJI.repeat(3), iconResId = R.drawable.ic_outline_audiotrack_24),
                    TimeLineData.Item(id = 2, laneIndex = 0, startMs = 10_000, stopMs = 20_000, label = SUSHI_EMOJI.repeat(3), iconResId = R.drawable.ic_outline_audiotrack_24),
                    TimeLineData.Item(id = 3, laneIndex = 1, startMs = 1000, stopMs = 2000, label = SUSHI_EMOJI, iconResId = R.drawable.ic_outline_audiotrack_24),
                    TimeLineData.Item(id = 4, laneIndex = 2, startMs = 0, stopMs = 2000, label = SUSHI_EMOJI, iconResId = R.drawable.ic_outline_audiotrack_24),
                    TimeLineData.Item(id = 5, laneIndex = 3, startMs = 1000, stopMs = 1500, label = SUSHI_EMOJI, iconResId = R.drawable.ic_outline_audiotrack_24),
                    TimeLineData.Item(id = 6, laneIndex = 4, startMs = 10_000, stopMs = 11_000, label = SUSHI_EMOJI, iconResId = R.drawable.ic_outline_audiotrack_24),
                )
            )
        )
    }

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
    ) {
        TimeLine(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            timeLineData = sushiData.value,
            currentPositionMs = currentPosition.longValue,
            onSeek = { positionMs -> currentPosition.longValue = positionMs },
            onDragAndDropRequest = { request ->
                // 位置更新のみ、入るかの判定はしていない。
                sushiData.value = sushiData.value.copy(
                    itemList = sushiData.value.itemList.map { sushi ->
                        if (sushi.id == request.id) {
                            sushi.copy(
                                laneIndex = request.dragAndDroppedLaneIndex,
                                startMs = request.dragAndDroppedStartMs,
                                stopMs = request.dragAndDroppedStartMs + (sushi.stopMs - sushi.startMs)
                            )
                        } else sushi
                    }
                )
                true
            }
        )
    }
}