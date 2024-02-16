package io.github.takusan23.akaridroid.v2.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.v2.RenderData
import io.github.takusan23.akaridroid.v2.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.v2.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.v2.ui.component.RenderItemPositionEditComponent

/**
 * [RenderData.CanvasItem.Video]の編集ボトムシート
 */
@Composable
fun VideoRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Video,
    isEdit: Boolean,
    onCreateOrUpdate: (RenderData.CanvasItem.Video) -> Unit,
    onDelete: (RenderData.CanvasItem.Video) -> Unit,
) {
    val videoItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.CanvasItem.Video) -> RenderData.CanvasItem.Video) {
        videoItem.value = copy(videoItem.value)
    }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = if (isEdit) "テキストの追加" else "テキストの編集",
            isEdit = isEdit,
            onCreateOrUpdate = { onCreateOrUpdate(videoItem.value) },
            onDelete = { onDelete(videoItem.value) }
        )

        RenderItemPositionEditComponent(
            position = videoItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = videoItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )
    }
}