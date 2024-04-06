package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
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
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_shape_title),
            onComplete = { onUpdate(shapeItem.value) },
            onDelete = { onDelete(shapeItem.value) }
        )

        OutlinedDropDownMenu(
            label = stringResource(id = R.string.video_edit_bottomsheet_shape_select),
            modifier = Modifier.fillMaxWidth(),
            currentSelectIndex = RenderData.CanvasItem.Shape.ShapeType.entries.indexOf(shapeItem.value.shapeType),
            menuList = listOf(
                stringResource(id = R.string.video_edit_bottomsheet_shape_rect),
                stringResource(id = R.string.video_edit_bottomsheet_shape_circle)
            ),
            onSelect = { index -> update { it.copy(shapeType = RenderData.CanvasItem.Shape.ShapeType.entries[index]) } }
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
