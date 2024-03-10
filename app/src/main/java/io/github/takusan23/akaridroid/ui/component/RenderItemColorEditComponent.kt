package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.tool.ColorTool

/** [RenderData.CanvasItem.Text.fontColor] 等の、色を編集する設定項目。 */
@Composable
fun RenderItemColorEditComponent(
    modifier: Modifier = Modifier,
    hexColorCode: String,
    onUpdate: (String) -> Unit
) {
    // パースできなかったら白
    val colorOrDefault = remember(hexColorCode) { mutableStateOf(ColorTool.parseColor(hexColorCode) ?: Color.White) }
    val isShowColorDialog = remember { mutableStateOf(false) }

    // ダイアログを出す
    if (isShowColorDialog.value) {
        ColorPickerDialog(
            currentColor = colorOrDefault.value,
            onDismissRequest = { isShowColorDialog.value = false },
            onChange = { color -> onUpdate(ColorTool.toHexColorCode(color)) }
        )
    }

    Row(
        modifier = modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(id = R.drawable.ic_outline_format_color_fill_24px),
            contentDescription = null
        )

        Text(
            modifier = Modifier.weight(1f),
            text = "色"
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colorOrDefault.value)
                .border(width = 1.dp, color = LocalContentColor.current)
        )

        Text(text = ColorTool.toHexColorCode(colorOrDefault.value))

        OutlinedButton(onClick = { isShowColorDialog.value = true }) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_format_color_fill_24px), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "色を変更")
        }
    }
}