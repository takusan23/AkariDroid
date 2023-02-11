package io.github.takusan23.akaridroid.ui.component

import android.view.SurfaceView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val aspectRatio = playerState.videoSizeData.collectAsState()
    val playerWidth = remember { mutableStateOf(0.dp) }
    val playerHeight = remember { mutableStateOf(0.dp) }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        // アスペクト比を自力で出す
        LaunchedEffect(key1 = this.maxWidth, key2 = aspectRatio.value) {
            val (height, width) = aspectRatio.value ?: return@LaunchedEffect
            if (height < width) {
                val aspect = height / width.toFloat()
                playerWidth.value = maxWidth
                playerHeight.value = playerWidth.value * aspect
            } else {
                val aspect = width / height.toFloat()
                playerHeight.value = maxHeight
                playerWidth.value = playerHeight.value * aspect
            }
        }

        AndroidView(
            modifier = Modifier
                .then(
                    if (playerWidth.value != 0.dp) {
                        Modifier
                            .height(playerHeight.value)
                            .width(playerWidth.value)
                    } else {
                        Modifier.aspectRatio(1.7f)
                    }
                ),
            factory = { context ->
                SurfaceView(context).apply {
                    playerState.setSurfaceView(this)
                }
            }
        )
    }
}