package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.RenderData

/**
 * [io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer]で素材を直感的に移動できるようにするやつ
 */
@Composable
fun TouchPreviewCanvas(
    modifier: Modifier = Modifier,
    previewBitmap: ImageBitmap?,

    // TODO TouchPreviewData とかで RenderData.CanvasItem 自体をもらうのはやめる
    canvasItemList: List<RenderData.CanvasItem>
) {
    // アスペクト比
    val aspectRate = remember(previewBitmap) { previewBitmap?.let { it.width / it.height.toFloat() } ?: 1f }


    val canvasItemList = remember(canvasItemList) { mutableStateOf(canvasItemList) }

    LaunchedEffect(key1 = canvasItemList.value) {
        println(canvasItemList.value)
    }

    // 画面サイズにプレビューが入るようにスケールする
    // Image とかはいい感じにやってくれるのですが、タッチイベントの処理の部分は自前で...
    val previewComponentSize = remember { mutableStateOf<IntSize?>(null) }

    Box(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.primary)
            .onSizeChanged { previewComponentSize.value = it },
        contentAlignment = Alignment.Center
    ) {
        if (previewBitmap != null) {
            Image(
                modifier = Modifier.matchParentSize(),
                bitmap = previewBitmap,
                contentDescription = null
            )
        } else {
            Text(text = "生成中です...")
        }

        // タッチイベントを処理する
        val canvasWidth = 1280
        val canvasHeight = 720
        val touchPreviewScale = previewComponentSize.value?.let { size ->
            if (canvasHeight < canvasWidth) {
                previewComponentSize.value!!.width / canvasWidth.toFloat()
            } else {
                canvasWidth.toFloat() / previewComponentSize.value!!.width
            }
        } ?: 1f

        Box(
            modifier = Modifier
                // TODO やめるこれ
                .requiredWidth(canvasWidth.pxToDp())
                .requiredHeight(canvasHeight.pxToDp())
                // requiredWidth だと画面外にはみ出す場合があるので、scale を使って画面内に収まるように調整する
                .scale(touchPreviewScale)
                // todo 消す
                .background(Color.Red.copy(0.1f))
        ) {
            canvasItemList.value.forEach { canvasItem ->

                val sizePx = when (canvasItem) {
                    is RenderData.CanvasItem.Image -> canvasItem.size
                    is RenderData.CanvasItem.Text -> canvasItem.measureSize()
                    is RenderData.CanvasItem.Video -> canvasItem.size
                }

                val offset = remember(canvasItem.position) {
                    mutableStateOf(
                        IntOffset(
                            x = canvasItem.position.x.toInt(),
                            y = canvasItem.position.y.toInt()
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .width(sizePx.width.pxToDp())
                        .height(sizePx.height.pxToDp())
                        .offset { offset.value }
                        .border(1.dp, Color.Yellow, RectangleShape)
                        .pointerInput(canvasItem) {
                            var moveItemId: Long? = null
                            detectDragGestures(
                                onDragStart = {
                                    moveItemId = canvasItem.id
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offset.value = IntOffset(
                                        x = (offset.value.x + dragAmount.x).toInt(),
                                        y = (offset.value.y + dragAmount.y).toInt()
                                    )
                                },
                                onDragEnd = {
                                    canvasItemList.value = canvasItemList.value.map {
                                        if (it.id == moveItemId) {
                                            val position = it.position.copy(
                                                x = offset.value.x * touchPreviewScale,
                                                y = offset.value.y * touchPreviewScale
                                            )
                                            when (it) {
                                                is RenderData.CanvasItem.Image -> it.copy(position = position)
                                                is RenderData.CanvasItem.Text -> it.copy(position = position)
                                                is RenderData.CanvasItem.Video -> it.copy(position = position)
                                            }
                                        } else {
                                            it
                                        }
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}

@Composable
private fun RenderData.CanvasItem.Text.measureSize(): RenderData.Size {
    val textMeasurer = rememberTextMeasurer()
    val pxToSp = with(LocalDensity.current) { textSize.toSp() }
    val size = textMeasurer.measure(this.text, TextStyle(fontSize = pxToSp)).size
    return RenderData.Size(size.width, size.height)
}

@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }
