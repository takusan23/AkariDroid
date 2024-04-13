package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.takusan23.akaridroid.R

/**
 * タイムラインの拡大、縮小ボタン
 *
 * @param modifier [Modifier]
 * @param msWidthPx 今の値
 * @param onZoomIn 拡大
 * @param onZoomOut 縮小
 */
@Composable
fun TimeLineZoomButtons(
    modifier: Modifier = Modifier,
    msWidthPx: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onZoomOut) {
            Icon(painter = painterResource(id = R.drawable.ic_zoom_out_24px), contentDescription = null)
        }

        Text(text = msWidthPx.toString())

        IconButton(onClick = onZoomIn) {
            Icon(painter = painterResource(id = R.drawable.ic_zoom_in_24px), contentDescription = null)
        }
    }
}