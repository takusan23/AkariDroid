package io.github.takusan23.akaridroid.ui.component.timeline

import android.app.Activity
import android.content.ClipData
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.DragAndDropPermissionsCompat

/** 受け入れる MIME-Type のプレフィックス*/
private val RECEIVE_MIME_TYPE_PREFIX_LIST = arrayOf("image/", "video/", "audio/")

/**
 * ファイルをドラッグアンドドロップで受け入れるコンテナ
 *
 * @param modifier [Modifier]
 * @param supportMimeTypePrefixList 対応している MIME-Type のプレフィックス一覧
 * @param onReceive ファイルのドラッグアンドドロップを受け入れた時。処理が終わったら[DragAndDropPermissionsCompat.release]してください。
 * @param content コンテナの中身の UI
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileDragAndDropReceiveContainer(
    modifier: Modifier = Modifier,
    supportMimeTypePrefixList: Array<String> = RECEIVE_MIME_TYPE_PREFIX_LIST,
    onReceive: (ClipData, DragAndDropPermissionsCompat) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    // ドラッグアンドドロップの操作中は true
    // 枠に色を付けたり
    val isProgressDragAndDrop = remember { mutableStateOf(false) }

    val callback = remember {
        object : DragAndDropTarget {

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val androidEvent = event.toAndroidDragEvent()
                val clipData = androidEvent.clipData

                // Uri にアクセスしますよ、requestDragAndDropPermissions を呼ぶ
                val dropPermissions = ActivityCompat.requestDragAndDropPermissions(context as Activity, androidEvent)
                if (dropPermissions != null) {
                    onReceive(clipData, dropPermissions)
                }
                return true
            }

            override fun onStarted(event: DragAndDropEvent) {
                super.onStarted(event)
                isProgressDragAndDrop.value = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                super.onEnded(event)
                isProgressDragAndDrop.value = false
            }

        }
    }


    Box(
        modifier = modifier
            .then(
                if (isProgressDragAndDrop.value) {

                    // 枠の色。アニメーションするよう
                    val targetColor = MaterialTheme.colorScheme.primary
                    val infiniteTransition = rememberInfiniteTransition()
                    val animateBorderColor = infiniteTransition.animateColor(
                        initialValue = Color.Transparent,
                        targetValue = targetColor,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Modifier
                        .border(
                            width = 10.dp,
                            color = animateBorderColor.value
                        )
                        .background(
                            color = targetColor.copy(alpha = 0.3f)
                        )
                } else Modifier
            )
            .dragAndDropTarget(
                shouldStartDragAndDrop = {
                    it
                        .mimeTypes()
                        .any { receiveMimeType ->
                            supportMimeTypePrefixList.any { supportMimeTypePrefix ->
                                receiveMimeType.startsWith(supportMimeTypePrefix)
                            }
                        }
                },
                target = callback
            ),
        content = content
    )
}