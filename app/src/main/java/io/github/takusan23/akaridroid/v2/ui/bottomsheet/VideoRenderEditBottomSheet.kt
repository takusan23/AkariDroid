package io.github.takusan23.akaridroid.v2.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.takusan23.akaridroid.v2.RenderData
import io.github.takusan23.akaridroid.v2.tool.UriTool
import io.github.takusan23.akaridroid.v2.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.v2.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.v2.ui.component.RenderItemPositionEditComponent
import io.github.takusan23.akaridroid.v2.ui.component.RenderItemSizeEditComponent

/**
 * [RenderData.CanvasItem.Video]の編集ボトムシート
 *
 * @param renderItem 動画素材の情報
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun VideoRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Video,
    onUpdate: (RenderData.CanvasItem.Video) -> Unit,
    onDelete: (RenderData.CanvasItem.Video) -> Unit,
) {
    val context = LocalContext.current
    val videoItem = remember { mutableStateOf(renderItem) }
    val videoFileName = remember { mutableStateOf<String?>(null) }

    fun update(copy: (RenderData.CanvasItem.Video) -> RenderData.CanvasItem.Video) {
        videoItem.value = copy(videoItem.value)
    }

    LaunchedEffect(key1 = videoItem.value.filePath) {
        // Uri しか来ないので
        (videoItem.value.filePath as? RenderData.FilePath.Uri)?.also { filePath ->
            videoFileName.value = UriTool.getFileName(context, filePath.uriPath.toUri())
        }
    }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = "映像の編集",
            onComplete = { onUpdate(videoItem.value) },
            onDelete = { onDelete(videoItem.value) }
        )

        Text(text = "ファイル名 : ${videoFileName.value}")

        RenderItemPositionEditComponent(
            position = videoItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = videoItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )

        // TODO Uri からサイズを取り出していれる機能
        RenderItemSizeEditComponent(
            size = videoItem.value.size,
            onUpdate = { size -> update { it.copy(size = size) } }
        )
    }
}