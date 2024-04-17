package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * プレビューの動画操作用パネル
 *
 * @param modifier [Modifier]
 * @param playerStatus 再生状態。再生位置とか再生中かとか
 * @param onPlayOrPause 再生か一時停止を押した時
 * @param onSeek シークしたとき
 */
@Composable
fun PreviewControlPanel(
    modifier: Modifier = Modifier,
    playerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    onPlayOrPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val currentPositionProgress = remember { mutableStateOf(0f) }
    val simpleDateFormat = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }

    val isControllerVisible = remember { mutableStateOf(true) }
    val currentTimeText = simpleDateFormat.format(playerStatus.currentPositionMs)
    val durationText = simpleDateFormat.format(playerStatus.durationMs)

    LaunchedEffect(key1 = playerStatus) {
        currentPositionProgress.value = playerStatus.currentPositionMs / playerStatus.durationMs.toFloat()
    }

    Surface(
        modifier = modifier,
        color = if (isControllerVisible.value) Color.Black.copy(alpha = 0.5f) else Color.Transparent,
        onClick = { isControllerVisible.value = !isControllerVisible.value },
        contentColor = Color.White
    ) {

        // 押したら消せるように
        if (!isControllerVisible.value) {
            return@Surface
        }

        Column {

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // 最初に戻る
                IconButton(onClick = { onSeek(0) }) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(id = R.drawable.ic_outlined_skip_previous_24px),
                        contentDescription = null
                    )
                }

                // 再生ボタン
                IconButton(
                    onClick = onPlayOrPause,
                    enabled = playerStatus.isPrepareCompleteAudio && playerStatus.isPrepareCompleteCanvas
                ) {
                    Icon(
                        modifier = Modifier.size(80.dp),
                        painter = painterResource(
                            id = when {
                                // プレビュー用意中はアイコンを砂時計にする。
                                // Windows XP のときは砂時計でしたね、なつかしい。応答なしに陥った時にウィンドウを動かすと残像が出たけど今は出ないんだって。
                                !playerStatus.isPrepareCompleteAudio || !playerStatus.isPrepareCompleteCanvas -> R.drawable.ic_outline_hourglass_top_24
                                playerStatus.isPlaying -> R.drawable.ic_outline_pause_24
                                else -> R.drawable.ic_outline_play_arrow_24
                            }
                        ),
                        contentDescription = null
                    )
                }

                // 最後に進む
                IconButton(onClick = { onSeek(playerStatus.durationMs) }) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(id = R.drawable.ic_outlined_skip_next_24px),
                        contentDescription = null
                    )
                }
            }

            // 再生位置
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {

                Text(text = currentTimeText)

                Slider(
                    modifier = Modifier.weight(1f),
                    value = playerStatus.currentPositionMs / playerStatus.durationMs.toFloat(),
                    onValueChange = { seekFloat -> onSeek((seekFloat * playerStatus.durationMs).toLong()) }
                )

                Text(text = durationText)
            }
        }
    }
}