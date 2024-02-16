package io.github.takusan23.akaridroid.v2.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.v2.RenderData
import io.github.takusan23.akaridroid.v2.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.v2.ui.component.OutlinedFloatTextField
import io.github.takusan23.akaridroid.v2.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.v2.ui.component.RenderItemPositionEditComponent

/**
 * 文字を追加するボトムシート
 *
 * @param canvasItemText キャンバスのテキストの情報
 * @param isEdit 編集は true、新規作成は false
 * @param onCreateOrUpdate 作成か更新
 * @param onDelete 削除押したとき
 */
@Composable
fun TextRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Text,
    isEdit: Boolean,
    onCreateOrUpdate: (RenderData.CanvasItem.Text) -> Unit,
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
            title = if (isEdit) "テキストの追加" else "テキストの編集",
            isEdit = isEdit,
            onCreateOrUpdate = { onCreateOrUpdate(textItem.value) },
            onDelete = { onDelete(textItem.value) }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textItem.value.text,
            onValueChange = { text -> update { it.copy(text = text) } },
            label = { Text(text = "文字") }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textItem.value.fontColor,
            onValueChange = { color -> update { it.copy(fontColor = color) } },
            label = { Text(text = "文字の色（カラーコード）") }
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