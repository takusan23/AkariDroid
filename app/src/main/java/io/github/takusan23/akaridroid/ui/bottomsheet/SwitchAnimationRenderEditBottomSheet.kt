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
 * [RenderData.CanvasItem.SwitchAnimation]の編集ボトムシート
 *
 * @param renderItem 切り替えアニメーションの詳細
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun SwitchAnimationRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.SwitchAnimation,
    onUpdate: (RenderData.CanvasItem.SwitchAnimation) -> Unit,
    onDelete: (RenderData.CanvasItem.SwitchAnimation) -> Unit
) {
    val context = LocalContext.current
    val shaderItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.CanvasItem.SwitchAnimation) -> RenderData.CanvasItem.SwitchAnimation) {
        shaderItem.value = copy(shaderItem.value)
    }

    Column(
        modifier = Modifier
            .bottomSheetPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_switch_animation_title),
            onComplete = { onUpdate(shaderItem.value) },
            onDelete = { onDelete(shaderItem.value) }
        )

        OutlinedDropDownMenu(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.video_edit_bottomsheet_switch_animation_type),
            currentSelectIndex = RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.entries.indexOf(shaderItem.value.animationType),
            menuList = RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.entries.map {
                context.getString(
                    when (it) {
                        RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.FADE_IN_OUT -> R.string.video_edit_bottomsheet_switch_animation_type_fade_in_out
                        RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.FADE_IN_OUT_WHITE -> R.string.video_edit_bottomsheet_switch_animation_type_fade_in_out_white
                        RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.SLIDE -> R.string.video_edit_bottomsheet_switch_animation_type_slide
                        RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.BLUR -> R.string.video_edit_bottomsheet_switch_animation_type_blur
                    }
                )
            },
            onSelect = { index -> update { it.copy(animationType = RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.entries[index]) } }
        )

        RenderItemPositionEditComponent(
            position = shaderItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = shaderItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )

        RenderItemSizeEditComponent(
            size = shaderItem.value.size,
            onUpdate = { size -> update { it.copy(size = size) } }
        )
    }

}