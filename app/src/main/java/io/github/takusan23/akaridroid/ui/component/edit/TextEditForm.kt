package io.github.takusan23.akaridroid.ui.component.edit

import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.data.CanvasElementType

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
    // Int を カラーコード
    val colorCode = remember(textElement.color) { "#%06X".format((0xFFFFFF and textElement.color)) }

    Column(modifier = modifier) {

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            value = textElement.text,
            onValueChange = { onUpdate(textElement.copy(text = it)) },
            label = { Text(text = "テキスト") }
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                value = colorCode,
                onValueChange = { onUpdate(textElement.copy(color = Color.parseColor(it))) },
                label = { Text(text = "色") }
            )
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                value = textElement.fontSize.toString(),
                onValueChange = { onUpdate(textElement.copy(fontSize = it.toFloat())) },
                label = { Text(text = "フォントサイズ") }
            )
        }

    }

}