package io.github.takusan23.akaridroid.ui.component

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * JetpackCompose で SurfaceView を使う
 *
 * @param modifier [Modifier]
 * @param onCreateSurface [SurfaceHolder.Callback]参照
 * @param onChangeSurface [SurfaceHolder.Callback]参照
 * @param onDestroySurface [SurfaceHolder.Callback]参照
 */
@Composable
fun ComposeSurfaceView(
    modifier: Modifier = Modifier,
    onCreateSurface: (holder: SurfaceHolder) -> Unit,
    onChangeSurface: (holder: SurfaceHolder, format: Int, width: Int, height: Int) -> Unit,
    onDestroySurface: (holder: SurfaceHolder) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        onCreateSurface(holder)
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        onChangeSurface(holder, format, width, height)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        onDestroySurface(holder)
                    }
                })
            }
        }
    )
}