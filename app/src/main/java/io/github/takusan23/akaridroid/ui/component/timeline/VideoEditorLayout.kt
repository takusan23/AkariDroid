package io.github.takusan23.akaridroid.ui.component.timeline

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/** この範囲でサイズを変更できる */
private val ResizeableRange = 0.2f..0.8f

/** ドラッグできる棒のサイズ */
private val BarSize = 20.dp

/**
 * 動画編集画面のレイアウトを作るための親 View
 * Slot API で位置を決めるだけではなくサイズ変更機能を持つ
 *
 * @param modifier Modifier
 */
@Composable
fun VideoEditorCompactPortraitLayout(
    modifier: Modifier = Modifier,
    draggable: Boolean = true,
    preview: @Composable BoxScope.() -> Unit,
    timeline: @Composable BoxScope.() -> Unit
) {
    val percent = rememberSaveable { mutableFloatStateOf(0.5f) }

    Box(modifier) {
        Column(
            modifier = Modifier.matchParentSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.weight(percent.floatValue),
                contentAlignment = Alignment.Center,
                content = preview
            )
            // 棒が上に重なるので、棒の分だけスペースを空ける
            if (draggable) {
                Spacer(modifier = Modifier.height(BarSize))
            }
            Box(
                modifier = Modifier.weight(1f - percent.floatValue),
                contentAlignment = Alignment.Center,
                content = timeline
            )
        }

        // ドラッグできる UI を上に重ねる
        if (draggable) {
            OverlayHorizontalDragBar(
                modifier = Modifier.matchParentSize(),
                percent = percent.floatValue,
                onPercentChange = { newPercent -> percent.floatValue = newPercent }
            )
        }
    }
}

/**
 * 動画編集画面のレイアウトを作るための親 View
 * Slot API で位置を決めるだけではなくサイズ変更機能を持つ
 *
 * @param modifier Modifier
 * @param draggable ドラッグできるかどうか
 * @param preview プレビューの Composable
 * @param timeline タイムラインの Composable
 */
@Composable
fun VideoEditorCompactLandscapeLayout(
    modifier: Modifier = Modifier,
    draggable: Boolean = true,
    preview: @Composable BoxScope.() -> Unit,
    timeline: @Composable BoxScope.() -> Unit
) {
    val percent = rememberSaveable { mutableFloatStateOf(0.5f) }

    Box(modifier) {
        Row(
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(percent.floatValue),
                contentAlignment = Alignment.Center,
                content = preview
            )
            // 棒が上に重なるので、棒の分だけスペースを空ける
            if (draggable) {
                Spacer(modifier = Modifier.width(BarSize))
            }
            Box(
                modifier = Modifier.weight(1f - percent.floatValue),
                contentAlignment = Alignment.Center,
                content = timeline
            )
        }

        // ドラッグできる UI を上に重ねる
        if (draggable) {
            OverlayVerticalDragBar(
                modifier = Modifier.matchParentSize(),
                percent = percent.floatValue,
                onPercentChange = { newPercent -> percent.floatValue = newPercent }
            )
        }
    }
}

/**
 * 大画面用、動画編集画面のレイアウトを作るための親 View
 * Slot API で位置を決めるだけではなくサイズ変更機能を持
 *
 * @param modifier Modifier
 * @param draggable ドラッグできるかどうか
 * @param preview プレビューの Composable
 * @param menu メニューの Composable
 * @param timeline タイムラインの Composable
 */
@Composable
fun VideoEditorLargeScreenLayout(
    modifier: Modifier = Modifier,
    draggable: Boolean = true,
    preview: @Composable BoxScope.() -> Unit,
    menu: @Composable BoxScope.() -> Unit,
    timeline: @Composable BoxScope.() -> Unit
) {
    VideoEditorCompactPortraitLayout(
        modifier = modifier,
        draggable = draggable,
        preview = {
            VideoEditorCompactLandscapeLayout(
                modifier = Modifier.fillMaxSize(),
                draggable = draggable,
                preview = preview,
                timeline = menu
            )
        },
        timeline = timeline
    )
}

/**
 * 横長のドラッグできる棒のオーバーレイ
 *
 * @param modifier Modifier
 * @param percent 割合
 * @param onPercentChange 割合が変わったときのコールバック
 */
@Composable
private fun OverlayHorizontalDragBar(
    modifier: Modifier = Modifier,
    percent: Float,
    onPercentChange: (Float) -> Unit
) {
    val density = LocalDensity.current
    val parentSize = remember { mutableStateOf<IntSize?>(null) }
    val dragging = remember { mutableStateOf(false) }

    Box(modifier = modifier.onSizeChanged { parentSize.value = it }) {
        // 親のサイズから棒の分を抜く
        val height = parentSize.value?.height?.let { it - with(density) { BarSize.toPx() } }
        if (height != null) {
            Box(
                modifier = Modifier
                    // offset { } で動かしてもよかった
                    .padding(top = with(density) { (percent * height).toDp() })
                    .pointerInput(key1 = Unit) {
                        var prevDragY = percent * height
                        var prevPercent = percent
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newDragY = prevDragY + dragAmount.y
                                val percent = (newDragY / height).coerceIn(ResizeableRange)
                                // 違うときのみ、違うのに newDragY が更新されないように
                                if (percent != prevPercent) {
                                    prevPercent = percent
                                    prevDragY = newDragY
                                    onPercentChange(percent)
                                }
                            },
                            onDragStart = { dragging.value = true },
                            onDragEnd = { dragging.value = false }
                        )
                    }
                    .fillMaxWidth()
                    .height(BarSize)
            ) {
                val barPercent = animateFloatAsState(targetValue = if (dragging.value) 0.5f else 0.3f)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.3f)
                        .fillMaxHeight(barPercent.value)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}

/**
 * 縦長のドラッグできる棒のオーバーレイ
 *
 * @param modifier Modifier
 * @param percent 割合
 * @param onPercentChange 割合が変わったときのコールバック
 */
@Composable
private fun OverlayVerticalDragBar(
    modifier: Modifier = Modifier,
    percent: Float,
    onPercentChange: (Float) -> Unit
) {
    val density = LocalDensity.current
    val parentSize = remember { mutableStateOf<IntSize?>(null) }
    val dragging = remember { mutableStateOf(false) }

    Box(modifier = modifier.onSizeChanged { parentSize.value = it }) {
        // 親のサイズから棒の分を抜く
        val width = parentSize.value?.width?.let { it - with(density) { BarSize.toPx() } }
        if (width != null) {
            Box(
                modifier = Modifier
                    // offset { } で動かしてもよかった
                    .padding(start = with(density) { (percent * width).toDp() })
                    .pointerInput(key1 = Unit) {
                        var prevDragX = percent * width
                        var prevPercent = percent
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newDragX = prevDragX + dragAmount.x
                                val percent = (newDragX / width).coerceIn(ResizeableRange)
                                // 違うときのみ、違うのに prevDragX が更新されないように
                                if (percent != prevPercent) {
                                    prevPercent = percent
                                    prevDragX = newDragX
                                    onPercentChange(percent)
                                }
                            },
                            onDragStart = { dragging.value = true },
                            onDragEnd = { dragging.value = false }
                        )
                    }
                    .fillMaxHeight()
                    .width(BarSize)
            ) {
                val barPercent = animateFloatAsState(targetValue = if (dragging.value) 0.5f else 0.3f)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight(0.3f)
                        .fillMaxWidth(barPercent.value)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}