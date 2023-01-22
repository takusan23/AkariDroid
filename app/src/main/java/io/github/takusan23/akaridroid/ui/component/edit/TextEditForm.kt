package io.github.takusan23.akaridroid.ui.component.edit

import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.data.CanvasElementType
import io.github.takusan23.akaridroid.ui.component.InitValueTextField

/**
 * テキスト要素の編集フォーム
 *
 * @param modifier [Modifier]
 * @param textElement [CanvasElementType.TextElement]
 * @param onUpdate
 */
@ExperimentalMaterial3Api
@Composable
fun TextEditForm(
    modifier: Modifier = Modifier,
    textElement: CanvasElementType.TextElement,
    onUpdate: (CanvasElementType.TextElement) -> Unit
) {
    Column(modifier = modifier) {

        InitValueTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            initValue = textElement.text,
            label = { Text(text = "テキスト") },
            onValueChange = { onUpdate(textElement.copy(text = it)) }
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            InitValueTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                initValue = "#%06X".format((0xFFFFFF and textElement.color)),
                onValueChange = { value ->
                    runCatching {
                        Color.parseColor(value)
                    }.onSuccess { color -> onUpdate(textElement.copy(color = color)) }
                },
                label = { Text(text = "色") }
            )
            InitValueTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                initValue = textElement.fontSize.toString(),
                label = { Text(text = "フォントサイズ") },
                onValueChange = { value -> value.toFloatOrNull()?.also { onUpdate(textElement.copy(fontSize = it)) } }
            )
        }
    }
}