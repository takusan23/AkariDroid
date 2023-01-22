package io.github.takusan23.akaridroid.ui.component.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.ui.component.InitValueTextField

/**
 * 要素に共通する編集フォーム
 *
 * @param modifier [Modifier]
 * @param canvasElementData [CanvasElementData]
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除押したときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasElementEditForm(
    modifier: Modifier = Modifier,
    canvasElementData: CanvasElementData,
    onUpdate: (CanvasElementData) -> Unit,
    onDelete: (CanvasElementData) -> Unit,
) {
    Column(modifier = modifier) {

        Row(modifier = Modifier.fillMaxWidth()) {
            InitValueTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                initValue = canvasElementData.xPos.toString(),
                label = { Text(text = "X座標") },
                onValueChange = { value -> value.toFloatOrNull()?.also { onUpdate(canvasElementData.copy(xPos = it)) } },
            )
            InitValueTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                initValue = canvasElementData.yPos.toString(),
                label = { Text(text = "Y座標") },
                onValueChange = { value -> value.toFloatOrNull()?.also { onUpdate(canvasElementData.copy(yPos = it)) } },
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            InitValueTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                initValue = canvasElementData.startMs.toString(),
                label = { Text(text = "描画開始時間 (ms)") },
                onValueChange = { value -> value.toLongOrNull()?.also { onUpdate(canvasElementData.copy(startMs = it)) } }
            )
            InitValueTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                initValue = canvasElementData.endMs.toString(),
                label = { Text(text = "描画終了時間 (ms)") },
                onValueChange = { value -> value.toLongOrNull()?.also { onUpdate(canvasElementData.copy(endMs = it)) } }
            )
        }

        Spacer(modifier = Modifier.height(50.dp))
        OutlinedButton(
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            onClick = { onDelete(canvasElementData) }
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24), contentDescription = null)
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = "削除する")
        }
    }
}