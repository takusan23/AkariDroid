package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import java.text.SimpleDateFormat
import java.util.Locale

/** [VideoEditorPreviewPlayer]を操作するためのシークバーとか */
@Composable
fun PreviewPlayerController(
    modifier: Modifier = Modifier,
    playerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    onSeek: (Long) -> Unit,
    onPlayOrPause: () -> Unit
) {
    val currentPositionProgress = remember { mutableStateOf(0f) }
    val simpleDateFormat = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }

    LaunchedEffect(key1 = playerStatus) {
        currentPositionProgress.value = playerStatus.currentPositionMs / playerStatus.durationMs.toFloat()
    }

    Surface(
        modifier = modifier,
        tonalElevation = 5.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        if (playerStatus.isProgressPreviewRender) {
            // 用意中
            Row(
                modifier = Modifier
                    .padding(5.dp)
                    .height(40.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // TODO LinearProgressIndicator とか置く。なんかバグってて今の Compose じゃエラーになる
                Text(
                    modifier = Modifier,
                    text = "プレビューの準備中です"
                )
            }
        } else {
            // 利用可能
            Row(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(onClick = onPlayOrPause) {
                    Icon(
                        painter = painterResource(id = if (playerStatus.isPlaying) R.drawable.ic_outline_pause_24 else R.drawable.ic_outline_play_arrow_24),
                        contentDescription = null
                    )
                }

                Text(text = simpleDateFormat.format(playerStatus.currentPositionMs))
                Slider(
                    modifier = Modifier.weight(1f),
                    value = currentPositionProgress.value,
                    onValueChange = { currentPositionProgress.value = it },
                    onValueChangeFinished = { onSeek((currentPositionProgress.value * playerStatus.durationMs).toLong()) }
                )
                Text(text = simpleDateFormat.format(playerStatus.durationMs))
            }
        }

    }
}