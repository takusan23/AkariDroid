package io.github.takusan23.akaridroid.ui.component

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Jetpack Compose で SurfaceView 使うため
 *
 * @param modifier [Modifier]
 * @param onCreateSurface [SurfaceHolder.Callback.surfaceCreated]
 * @param onSizeChanged [SurfaceHolder.Callback.surfaceChanged]
 * @param onDestroySurface [SurfaceHolder.Callback.surfaceDestroyed]
 */
@Composable
fun ComposeSurfaceView(
    modifier: Modifier = Modifier,
    onCreateSurface: (SurfaceHolder) -> Unit,
    onSizeChanged: (width: Int, height: Int) -> Unit,
    onDestroySurface: () -> Unit
) {
    AndroidView(
        modifier = modifier.clipToBounds(), // Android 11 以前で AndroidView + SurfaceView すると背景が真っ暗になるので必要
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) = onCreateSurface(holder)
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = onSizeChanged(width, height)
                    override fun surfaceDestroyed(holder: SurfaceHolder) = onDestroySurface()
                })
            }
        }
    )
}