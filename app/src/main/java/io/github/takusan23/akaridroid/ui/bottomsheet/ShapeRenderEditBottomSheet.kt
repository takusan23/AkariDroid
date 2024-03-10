package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.OutlinedDropDownMenu
import io.github.takusan23.akaridroid.ui.component.RenderItemColorEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemPositionEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemSizeEditComponent

/**
 * [RenderData.CanvasItem.Shape]の編集ボトムシート
 *
 * @param renderItem 図形の情報
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun ShapeRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Shape,
    onUpdate: (RenderData.CanvasItem.Shape) -> Unit,
    onDelete: (RenderData.CanvasItem.Shape) -> Unit
) {
    val shapeItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.CanvasItem.Shape) -> RenderData.CanvasItem.Shape) {
        shapeItem.value = copy(shapeItem.value)
    }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = "図形の編集",
            onComplete = { onUpdate(shapeItem.value) },
            onDelete = { onDelete(shapeItem.value) }
        )

        OutlinedDropDownMenu(
            label = "図形の種類",
            modifier = Modifier.fillMaxWidth(),
            currentSelectIndex = RenderData.CanvasItem.Shape.Type.entries.indexOf(shapeItem.value.type),
            menuList = listOf("四角", "丸"),
            onSelect = { index -> update { it.copy(type = RenderData.CanvasItem.Shape.Type.entries[index]) } }
        )

        RenderItemColorEditComponent(
            modifier = Modifier.fillMaxWidth(),
            hexColorCode = shapeItem.value.color,
            onUpdate = { color -> update { it.copy(color = color) } }
        )

        RenderItemPositionEditComponent(
            position = shapeItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = shapeItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )

        RenderItemSizeEditComponent(
            size = shapeItem.value.size,
            onUpdate = { size -> update { it.copy(size = size) } }
        )
    }
}
