package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.MessageCard
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemPositionEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemSizeEditComponent

/**
 * [RenderData.CanvasItem.Shader]の編集ボトムシート
 *
 * @param renderItem シェーダーを含む情報
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun ShaderRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Shader,
    onUpdate: (RenderData.CanvasItem.Shader) -> Unit,
    onDelete: (RenderData.CanvasItem.Shader) -> Unit
) {
    val shaderItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.CanvasItem.Shader) -> RenderData.CanvasItem.Shader) {
        shaderItem.value = copy(shaderItem.value)
    }

    Column(
        modifier = Modifier
            .bottomSheetPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_shader_title),
            onComplete = { onUpdate(shaderItem.value) },
            onDelete = { onDelete(shaderItem.value) }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = shaderItem.value.name,
            onValueChange = { name -> update { it.copy(name = name) } },
            label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_shader_name)) },
            maxLines = 1
        )

        // TODO シェーダーがコンパイルできるかチェック
        // TODO コピペボタン
        // TODO リセットボタン

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = shaderItem.value.fragmentShader,
            onValueChange = { fragmentShader -> update { it.copy(fragmentShader = fragmentShader) } },
            label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_shader_fragment_shader)) }
        )

        MessageCard(message = stringResource(id = R.string.video_edit_bottomsheet_shader_description))

        MessageCard(message = stringResource(id = R.string.video_edit_bottomsheet_shader_sorry))

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