package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.tool.UriTool
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemPositionEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemSizeEditComponent

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
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_video_title),
            onComplete = { onUpdate(videoItem.value) },
            onDelete = { onDelete(videoItem.value) }
        )

        Text(text = "${stringResource(id = R.string.video_edit_bottomsheet_video_file_name)} : ${videoFileName.value}")

        RenderItemPositionEditComponent(
            position = videoItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = videoItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )

        RenderItemSizeEditComponent(
            size = videoItem.value.size,
            onUpdate = { size -> update { it.copy(size = size) } }
        )
    }
}