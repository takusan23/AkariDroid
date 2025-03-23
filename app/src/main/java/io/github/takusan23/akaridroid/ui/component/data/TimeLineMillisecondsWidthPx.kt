package io.github.takusan23.akaridroid.ui.component.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * タイムラインの拡大縮小用
 *
 * @param msWidthPx 1 ミリ秒をどれだけの幅で表すか
 */
@JvmInline
value class TimeLineMillisecondsWidthPx(val msWidthPx: Int = 20) {

    /** 1 ミリ秒をどれだけの幅で表すか。[dp]版 */
    val Long.msToWidthDp: Dp
        // Pixel to Dp
        @Composable
        get() = with(LocalDensity.current) { msToWidth.toDp() }

    /** 1 ミリ秒をどれだけの幅で表すか。[Int]版。 */
    val Long.msToWidth: Int
        get() = (this / msWidthPx).toInt()

    /** 幅や位置は何 ミリ秒を表しているか */
    val Int.widthToMs: Long
        get() = (this * msWidthPx).toLong()

    /** 幅や位置は何 ミリ秒を表しているか */
    val Float.widthToMs: Long
        get() = (this * msWidthPx).toLong()

}
