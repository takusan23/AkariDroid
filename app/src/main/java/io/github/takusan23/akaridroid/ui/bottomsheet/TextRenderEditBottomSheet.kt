package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.OutlinedFloatTextField
import io.github.takusan23.akaridroid.ui.component.RenderItemColorEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemPositionEditComponent

/**
 * [RenderData.CanvasItem.Text]の編集ボトムシート
 *
 * @param renderItem キャンバスのテキストの情報
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun TextRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Text,
    onUpdate: (RenderData.CanvasItem.Text) -> Unit,
    onDelete: (RenderData.CanvasItem.Text) -> Unit,
) {
    val textItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.CanvasItem.Text) -> RenderData.CanvasItem.Text) {
        textItem.value = copy(textItem.value)
    }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = "テキストの編集",
            onComplete = { onUpdate(textItem.value) },
            onDelete = { onDelete(textItem.value) }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textItem.value.text,
            onValueChange = { text -> update { it.copy(text = text) } },
            label = { Text(text = "文字") }
        )

        RenderItemColorEditComponent(
            modifier = Modifier.fillMaxWidth(),
            hexColorCode = textItem.value.fontColor,
            onUpdate = { color -> update { it.copy(fontColor = color) } }
        )

        StrokeTextEditComponent(
            strokeColor = textItem.value.strokeColor,
            onUpdate = { color -> update { it.copy(strokeColor = color) } }
        )

        OutlinedFloatTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textItem.value.textSize,
            onValueChange = { textSize -> update { it.copy(textSize = textSize) } },
            label = { Text(text = "文字サイズ") }
        )

        RenderItemPositionEditComponent(
            position = textItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = textItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )
    }
}

/** 枠取り文字にするなら */
@Composable
private fun StrokeTextEditComponent(
    strokeColor: String?,
    onUpdate: (String?) -> Unit
) {
    val isShowStrokeColor = remember { mutableStateOf(strokeColor != null) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_text_fields_24), contentDescription = null)
            Text(
                modifier = Modifier.weight(1f),
                text = "枠取り文字にする"
            )
            Switch(
                checked = isShowStrokeColor.value,
                onCheckedChange = {
                    isShowStrokeColor.value = it
                    onUpdate(if (it) "#000000" else null)
                }
            )
        }

        if (isShowStrokeColor.value) {
            RenderItemColorEditComponent(
                modifier = Modifier.fillMaxWidth(),
                hexColorCode = strokeColor!!,
                onUpdate = { color -> onUpdate(color) }
            )
        }
    }
}