package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * [VideoEditorPreviewPlayer]のコントローラー。再生、一時停止、最初に戻るなど。
 *
 * @param modifier [Modifier]
 * @param playerStatus 再生状態
 * @param onPlayOrPause 再生、一時停止が変化した
 */
@Composable
fun PreviewPlayerController(
    modifier: Modifier = Modifier,
    playerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    onPlayOrPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val currentPositionProgress = remember { mutableStateOf(0f) }
    val simpleDateFormat = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }

    LaunchedEffect(key1 = playerStatus) {
        currentPositionProgress.value = playerStatus.currentPositionMs / playerStatus.durationMs.toFloat()
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        // 再生ボタン
        OutlinedIconButton(
            onClick = onPlayOrPause,
            enabled = playerStatus.isPrepareCompleteAudio && playerStatus.isPrepareCompleteCanvas
        ) {
            Icon(
                painter = painterResource(id = if (playerStatus.isPlaying) R.drawable.ic_outline_pause_24 else R.drawable.ic_outline_play_arrow_24),
                contentDescription = null
            )
        }

        // 最初に戻る
        IconButton(onClick = { onSeek(0) }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outlined_skip_previous_24px),
                contentDescription = null
            )
        }

        // 最後に進む
        IconButton(onClick = { onSeek(playerStatus.durationMs) }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outlined_skip_next_24px),
                contentDescription = null
            )
        }

        // 再生位置
        val currentTimeText = simpleDateFormat.format(playerStatus.currentPositionMs)
        val durationText = simpleDateFormat.format(playerStatus.durationMs)
        Text(text = "$currentTimeText / $durationText")
    }
}