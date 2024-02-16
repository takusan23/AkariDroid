package io.github.takusan23.akaridroid.v2.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.takusan23.akaridroid.v2.RenderData

/** [RenderData.DisplayTime]を編集する共通コンポーネント */
@Composable
fun RenderItemDisplayTimeEditComponent(
    displayTime: RenderData.DisplayTime,
    onUpdate:(RenderData.DisplayTime)->Unit
) {
    Row {
        OutlinedLongTextField(
            modifier = Modifier.weight(1f),
            value = displayTime.startMs,
            onValueChange = { startMs -> onUpdate(displayTime.copy(startMs = startMs)) },
            label = { Text(text = "表示開始時間") }
        )
        OutlinedLongTextField(
            modifier = Modifier.weight(1f),
            value = displayTime.stopMs,
            onValueChange = { stopMs -> onUpdate(displayTime.copy(stopMs = stopMs)) },
            label = { Text(text = "表示終了時間") }
        )
    }
}