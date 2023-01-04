package io.github.takusan23.akaridroid.ui.component

import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.takusan23.akaridroid.ui.tool.VideoPlayerState

/**
 * 動画プレイヤー
 *
 * @param modifier [Modifier]
 * @param playerState 動画プレイヤー
 */
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    playerState: VideoPlayerState
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceView(context).apply {
                playerState.setSurfaceView(this)
            }
        }
    )
}