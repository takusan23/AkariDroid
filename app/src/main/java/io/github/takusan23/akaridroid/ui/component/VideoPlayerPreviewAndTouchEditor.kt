package io.github.takusan23.akaridroid.ui.component

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.data.TouchEditorData

/** ピクセル単位を DP に変換する */
@Composable
private fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

/** 移動中、サイズ変更中に文字を出す。 */
private enum class TouchEditorItemTextDescriptionType {
    /** サイズ変更をテキストで補足 */
    Size,

    /** 位置変更をテキストで補足 */
    Position
}

/**
 * [io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer]
 * プレビューを表示する機能と、キャンバス要素をタッチで移動できるようにするやつ
 *
 * @param modifier [Modifier]
 * @param previewBitmap プレビューBitmap。[io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer.previewBitmap]
 * @param touchEditorData 現在表示されているキャンバス要素を[TouchEditorData]で
 * @param onDragAndDropEnd タッチ操作で移動が終わったら呼ばれる。[TouchEditorData.PositionUpdateRequest]
 * @param onSizeChangeRequest ピンチイン、ピンチアウトでサイズ変更されたら呼ばれる。[TouchEditorData.SizeChangeRequest]
 */
@Composable
fun VideoPlayerPreviewAndTouchEditor(
    modifier: Modifier = Modifier,
    previewBitmap: ImageBitmap?,
    touchEditorData: TouchEditorData,
    onDragAndDropEnd: (TouchEditorData.PositionUpdateRequest) -> Unit,
    onSizeChangeRequest: (TouchEditorData.SizeChangeRequest) -> Unit
) {
    // タッチ移動機能の ON/OFF
    val isEnableTouchEditor = remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        // プレビューを出す
        if (previewBitmap != null) {
            Image(
                modifier = Modifier
                    .matchParentSize()
                    .align(Alignment.Center),
                bitmap = previewBitmap,
                contentDescription = null
            )
        } else {
            Text(text = "生成中です...")
        }

        // キャンバス要素をドラッグアンドドロップで移動できるように
        // ON/OFF 切り替えできるように
        if (isEnableTouchEditor.value) {
            TouchEditor(
                modifier = Modifier
                    .matchParentSize()
                    .align(Alignment.Center),
                videoSize = touchEditorData.videoSize,
                touchEditorItemList = touchEditorData.visibleTouchEditorItemList,
                onDragAndDropEnd = onDragAndDropEnd,
                onSizeChangeRequest = onSizeChangeRequest
            )
        }

        // タッチ移動モードスイッチ
        TouchEditSwitch(
            modifier = Modifier.align(Alignment.TopEnd),
            isEnable = isEnableTouchEditor.value,
            onChange = { isEnableTouchEditor.value = it }
        )

        // おしらせ（プレビューがとても遅いけど、出力には関係ないよ）
        PreviewNotice(modifier = Modifier.align(Alignment.TopStart))
    }
}

/**
 * キャンバス要素自体をタッチ操作で直感的に移動できるようにする
 *
 * @param modifier [Modifier]
 * @param videoSize 動画のサイズ。実際は動画のサイズに収まるように表示はスケールされます。
 * @param touchEditorItemList キャンバス要素。再生位置に合わせて必要なやつだけ。
 * @param onDragAndDropEnd タッチ操作で移動が終わったら呼ばれる。[TouchEditorData.PositionUpdateRequest]
 * @param onSizeChangeRequest ピンチイン、ピンチアウトでサイズ変更されたら呼ばれる。[TouchEditorData.SizeChangeRequest]
 */
@Composable
private fun TouchEditor(
    modifier: Modifier = Modifier,
    videoSize: RenderData.Size,
    touchEditorItemList: List<TouchEditorData.TouchEditorItem>,
    onDragAndDropEnd: (TouchEditorData.PositionUpdateRequest) -> Unit,
    onSizeChangeRequest: (TouchEditorData.SizeChangeRequest) -> Unit
) {
    val parentComponentSize = remember { mutableStateOf<IntSize?>(null) }

    // 親の大きさを出す
    Box(
        modifier = modifier.onSizeChanged { parentComponentSize.value = it },
        contentAlignment = Alignment.Center
    ) {
        if (parentComponentSize.value != null) {

            // 動画の縦横サイズと、実際のプレビューで使えるコンポーネントのサイズ。画面外にはみ出さないようにスケールを出す
            // 横動画と縦動画
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
            ) {
                // 枠線とドラッグできるやつ
                touchEditorItemList.forEach { previewItem ->
                    TouchEditorItem(
                        touchEditorItem = previewItem,
                        onDragAndDropEnd = onDragAndDropEnd,
                        onSizeChangeRequest = onSizeChangeRequest
                    )
                }
            }
        }
    }
}

/**
 * [TouchEditor]で表示するそれぞれのアイテム
 *
 * @param modifier [Modifier]
 * @param touchEditorItem キャンバス要素[TouchEditorData.TouchEditorItem]
 * @param onDragAndDropEnd タッチ操作で移動が終わったら呼ばれる。[TouchEditorData.PositionUpdateRequest]
 * @param onSizeChangeRequest ピンチイン、ピンチアウトでサイズ変更されたら呼ばれる。[TouchEditorData.SizeChangeRequest]
 */
@Composable
private fun TouchEditorItem(
    modifier: Modifier = Modifier,
    touchEditorItem: TouchEditorData.TouchEditorItem,
    onDragAndDropEnd: (TouchEditorData.PositionUpdateRequest) -> Unit,
    onSizeChangeRequest: (TouchEditorData.SizeChangeRequest) -> Unit
) {
    // 動かしてるときの位置
    val offset = remember(touchEditorItem) {
        mutableStateOf(
            IntOffset(
                x = touchEditorItem.position.x.toInt(),
                y = touchEditorItem.position.y.toInt()
            )
        )
    }

    // 拡大率
    val itemWidth = remember(touchEditorItem) { mutableIntStateOf(touchEditorItem.size.width) }
    val itemHeight = remember(touchEditorItem) { mutableIntStateOf(touchEditorItem.size.height) }

    // 編集中に文字で補足する
    val textDescriptionType = remember(touchEditorItem) { mutableStateOf<TouchEditorItemTextDescriptionType?>(null) }

    Box(
        modifier = modifier
            // ピクセル単位で指定する必要あり
            .width(itemWidth.intValue.pxToDp())
            .height(itemHeight.intValue.pxToDp())
            // 移動位置を Offset で指定
            .offset { offset.value }
            .border(width = 1.dp, color = Color.Yellow, shape = RectangleShape)
            // ドラッグアンドドロップで移動させる
            .pointerInput(touchEditorItem) {
                detectDragGestures(
                    onDragStart = {
                        textDescriptionType.value = TouchEditorItemTextDescriptionType.Position
                    },
                    onDrag = { change, dragAmount ->
                        // 拡大縮小でイベントを使うかもしれないので、消費しない
                        change.consume()
                        offset.value = IntOffset(
                            x = (offset.value.x + dragAmount.x).toInt(),
                            y = (offset.value.y + dragAmount.y).toInt()
                        )
                    },
                    onDragEnd = {
                        // 終了時は上に伝える
                        onDragAndDropEnd(
                            TouchEditorData.PositionUpdateRequest(
                                id = touchEditorItem.id,
                                position = RenderData.Position(
                                    offset.value.x.toFloat(),
                                    offset.value.y.toFloat()
                                )
                            )
                        )
                        textDescriptionType.value = null
                    }
                )
            }
    ) {

        // テキストで補足するやつ。
        if (textDescriptionType.value != null) {
            TextDescription(
                modifier = Modifier.align(Alignment.BottomCenter),
                text = when (textDescriptionType.value!!) {
                    TouchEditorItemTextDescriptionType.Size -> "${itemWidth.intValue} x ${itemHeight.intValue}"
                    TouchEditorItemTextDescriptionType.Position -> "${offset.value.x} , ${offset.value.y}"
                }
            )
        }

        Icon(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(25.dp)
                .border(width = 1.dp, color = Color.Yellow, shape = RectangleShape)
                .background(Color.White)
                // 端っこをつまんでサイズ変更
                .pointerInput(touchEditorItem) {
                    val aspect = touchEditorItem.size.height / touchEditorItem.size.width.toFloat()
                    detectDragGestures(
                        onDragStart = {
                            textDescriptionType.value = TouchEditorItemTextDescriptionType.Size
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            itemWidth.intValue = (itemWidth.intValue + dragAmount.x).toInt()
                            itemHeight.intValue = (itemWidth.intValue * aspect).toInt()
                        },
                        onDragEnd = {
                            // サイズ変更
                            onSizeChangeRequest(
                                TouchEditorData.SizeChangeRequest(
                                    id = touchEditorItem.id,
                                    size = RenderData.Size(
                                        width = itemWidth.intValue,
                                        height = itemHeight.intValue
                                    )
                                )
                            )
                            textDescriptionType.value = null
                        }
                    )
                },
            painter = painterResource(id = R.drawable.ic_outline_pan_zoom_24px),
            contentDescription = null,
            tint = Color.Black
        )

    }
}

/**
 * タッチ移動モード変更スイッチ
 *
 * @param modifier [Modifier]
 * @param isEnable 有効かどうか
 * @param onChange 変更時
 */
@Composable
private fun TouchEditSwitch(
    modifier: Modifier = Modifier,
    isEnable: Boolean,
    onChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier,
        checked = isEnable,
        onCheckedChange = {
            val after = !isEnable
            onChange(after)

            // 意味分からんと思うので Toast も出す
            val toastMessage = if (after) "タッチ編集が有効になりました" else "タッチ編集が無効になりました"
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        },
        shape = RoundedCornerShape(5.dp)
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outlined_touch_app_24px),
                contentDescription = null
            )
            Switch(
                checked = isEnable,
                onCheckedChange = null
            )
        }
    }
}

/** プレビューがとても遅いけど、出力には問題ないよ */
@Composable
private fun PreviewNotice(modifier: Modifier = Modifier) {
    val isShow = remember { mutableStateOf(false) }

    // プレビューがとても遅いけど仕様だからごめん。
    if (isShow.value) {
        AlertDialog(
            onDismissRequest = { isShow.value = false },
            title = { Text(text = "プレビュー機能について") },
            text = {
                Text(
                    text = """
                    プレビューがとても遅いですが、出力には問題ありません。
                    使いにくいと思います。すいません。
                    いい方法が思いつけば直します。
                """.trimIndent()
                )
            },
            confirmButton = {
                Button(onClick = { isShow.value = false }) {
                    Text(text = "とじる")
                }
            }
        )
    }

    IconButton(
        modifier = modifier,
        onClick = { isShow.value = !isShow.value }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_outlined_info_24px),
            contentDescription = null
        )
    }
}

/**
 * テキストで補足するやつ。移動中や、サイズ変更など。
 *
 * @param modifier [Modifier]
 * @param text 補足するテキスト
 */
@Composable
private fun TextDescription(
    modifier: Modifier = Modifier,
    text: String
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(2.dp)
    ) {
        Text(
            modifier = modifier.padding(2.dp),
            text = text,
            maxLines = 1
        )
    }
}
