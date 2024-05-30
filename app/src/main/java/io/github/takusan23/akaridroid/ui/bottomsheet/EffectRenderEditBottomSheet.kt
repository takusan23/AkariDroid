package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.OutlinedDropDownMenu
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemPositionEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemSizeEditComponent

/**
 * [RenderData.CanvasItem.Effect]の編集ボトムシート
 *
 * @param renderItem 切り替えアニメーションの詳細
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun EffectRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Effect,
    onUpdate: (RenderData.CanvasItem.Effect) -> Unit,
    onDelete: (RenderData.CanvasItem.Effect) -> Unit
) {
    val context = LocalContext.current
    val effectItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.CanvasItem.Effect) -> RenderData.CanvasItem.Effect) {
        effectItem.value = copy(effectItem.value)
    }

    Column(
        modifier = Modifier
            .bottomSheetPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_effect_title),
            onComplete = { onUpdate(effectItem.value) },
            onDelete = { onDelete(effectItem.value) }
        )

        OutlinedDropDownMenu(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.video_edit_bottomsheet_effect_type),
            currentSelectIndex = RenderData.CanvasItem.Effect.EffectType.entries.indexOf(effectItem.value.effectType),
            menuList = RenderData.CanvasItem.Effect.EffectType.entries.map {
                context.getString(
                    when (it) {
                        RenderData.CanvasItem.Effect.EffectType.MOSAIC -> R.string.video_edit_bottomsheet_effect_type_mosaic
                        RenderData.CanvasItem.Effect.EffectType.MONOCHROME -> R.string.video_edit_bottomsheet_effect_type_monochrome
                        RenderData.CanvasItem.Effect.EffectType.THRESHOLD -> R.string.video_edit_bottomsheet_effect_type_threshold
                    }
                )
            },
            onSelect = { index -> update { it.copy(effectType = RenderData.CanvasItem.Effect.EffectType.entries[index]) } }
        )

        RenderItemPositionEditComponent(
            position = effectItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = effectItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )

        RenderItemSizeEditComponent(
            size = effectItem.value.size,
            onUpdate = { size -> update { it.copy(size = size) } }
        )
    }
}