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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.data.TouchPreviewData

/** ピクセル単位を DP に変換する */
@Composable
private fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

/**
 * [io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer]で素材を直感的に移動できるようにするやつ
 * あとプレビューを映し出す機能。
 * TODO タッチ編集機能付きプレビューコンポーネント とかに名前を変えたほうがいい
 */
@Composable
fun TouchPreviewCanvas(
    modifier: Modifier = Modifier,
    previewBitmap: ImageBitmap?,
    touchPreviewData: TouchPreviewData,
    onDragAndDropEnd: (TouchPreviewData.PositionUpdateRequest) -> Unit
) {
    Box(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        // プレビューを出す
        if (previewBitmap != null) {
            Image(
                modifier = Modifier.matchParentSize(),
                bitmap = previewBitmap,
                contentDescription = null
            )
        } else {
            Text(text = "生成中です...")
        }

        // キャンバス要素をドラッグアンドドロップで移動できるように
        TouchPreviewView(
            modifier = Modifier.matchParentSize(),
            videoSize = touchPreviewData.videoSize,
            touchPreviewItemList = touchPreviewData.visibleCanvasItemList,
            onDragAndDropEnd = onDragAndDropEnd
        )
    }
}

@Composable
private fun TouchPreviewView(
    modifier: Modifier = Modifier,
    videoSize: RenderData.Size,
    touchPreviewItemList: List<TouchPreviewData.TouchPreviewItem>,
    onDragAndDropEnd: (TouchPreviewData.PositionUpdateRequest) -> Unit
) {
    val parentComponentSize = remember { mutableStateOf<IntSize?>(null) }

    // 親の大きさを出す
    Box(
        modifier = modifier.onSizeChanged { parentComponentSize.value = it },
        contentAlignment = Alignment.Center
    ) {
        if (parentComponentSize.value != null) {

            // 動画の縦横サイズと、実際のプレビューで使えるコンポーネントのサイズ。画面外にはみ出さないようにスケールを出す
            val scale = if (videoSize.height < videoSize.width) {
                parentComponentSize.value!!.width / videoSize.width.toFloat()
            } else {
                parentComponentSize.value!!.height / videoSize.height.toFloat()
            }

            Box(
                modifier = Modifier
                    // 動画のサイズの Box を作る
                    .requiredWidth(videoSize.width.pxToDp())
                    .requiredHeight(videoSize.height.pxToDp())
                    // 動画のサイズを指定するとはみ出すので、scale を使って画面内に収まるように調整する
                    .scale(scale)
                    .background(Color.Red.copy(.5f))
            ) {
                // 枠線とドラッグできるやつ
                touchPreviewItemList.forEach { previewItem ->
                    TouchPreviewItem(
                        touchPreviewItem = previewItem,
                        onDragAndDropEnd = onDragAndDropEnd
                    )
                }
            }
        }
    }

}

@Composable
private fun TouchPreviewItem(
    modifier: Modifier = Modifier,
    touchPreviewItem: TouchPreviewData.TouchPreviewItem,
    onDragAndDropEnd: (TouchPreviewData.PositionUpdateRequest) -> Unit
) {
    // 動かしてるときの位置
    val offset = remember(touchPreviewItem) {
        mutableStateOf(
            IntOffset(
                x = touchPreviewItem.position.x.toInt(),
                y = touchPreviewItem.position.y.toInt()
            )
        )
    }

    Box(
        modifier = modifier
            // ピクセル単位で指定する必要あり
            .width(touchPreviewItem.size.width.pxToDp())
            .height(touchPreviewItem.size.height.pxToDp())
            // 移動位置を Offset で指定
            .offset { offset.value }
            .border(1.dp, Color.Yellow, RectangleShape)
            // ドラッグアンドドロップで移動させる
            .pointerInput(touchPreviewItem) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset.value = IntOffset(
                            x = (offset.value.x + dragAmount.x).toInt(),
                            y = (offset.value.y + dragAmount.y).toInt()
                        )
                    },
                    onDragEnd = {
                        // 終了時は上に伝える
                        onDragAndDropEnd(
                            TouchPreviewData.PositionUpdateRequest(
                                id = touchPreviewItem.id,
                                size = touchPreviewItem.size,
                                position = RenderData.Position(
                                    offset.value.x.toFloat(),
                                    offset.value.y.toFloat()
                                )
                            )
                        )
                    }
                )
            }
    )
}