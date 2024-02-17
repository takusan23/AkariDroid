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
import io.github.takusan23.akaridroid.v2.ui.component.RenderItemSizeEditComponent

/**
 * [RenderData.CanvasItem.Image]の編集ボトムシート
 *
 * @param renderItem 画像素材の情報
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun ImageRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Image,
    onUpdate: (RenderData.CanvasItem.Image) -> Unit,
    onDelete: (RenderData.CanvasItem.Image) -> Unit,
) {
    val imageItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.CanvasItem.Image) -> RenderData.CanvasItem.Image) {
        imageItem.value = copy(imageItem.value)
    }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = "画像の編集",
            onComplete = { onUpdate(imageItem.value) },
            onDelete = { onDelete(imageItem.value) }
        )

        RenderItemPositionEditComponent(
            position = imageItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = imageItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )

        // TODO Uri からサイズを取り出していれる機能
        RenderItemSizeEditComponent(
            size = imageItem.value.size,
            onUpdate = { size -> update { it.copy(size = size) } }
        )
    }
}