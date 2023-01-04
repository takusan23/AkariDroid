package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.tool.TimeFormatTool

/**
 * 動画プレイヤーの再生、一時停止部分
 *
 * @param modifier [Modifier]
 * @param isPlaying 再生中ならtrue
 * @param currentPosMs 再生位置
 * @param durationMs 動画時間
 * @param onPlay 再生状態が変わったら呼ばれる
 * @param onSeek シーク時に呼ばれる
 */
@Composable
fun VideoPlayerController(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    onPlay: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // シークバー
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(start = 5.dp, end = 5.dp),
                text = TimeFormatTool.videoDurationToFormatText(currentPosMs / 1000L)
            )
            Slider(
                modifier = Modifier.weight(1f),
                value = currentPosMs.toFloat(),
                valueRange = 0f..durationMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) }
            )
            Text(
                modifier = Modifier.padding(start = 5.dp, end = 5.dp),
                text = TimeFormatTool.videoDurationToFormatText(durationMs / 1000L)
            )
        }
        // 一時停止ボタン
        FilledIconToggleButton(
            modifier = Modifier.size(50.dp),
            checked = isPlaying,
            onCheckedChange = { onPlay(it) }
        ) {
            Icon(
                painter = painterResource(id = if (isPlaying) R.drawable.ic_outline_pause_24 else R.drawable.ic_outline_play_arrow_24),
                contentDescription = null
            )
        }
    }
}