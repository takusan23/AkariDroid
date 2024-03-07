package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.RenderData

/** [RenderData.DisplayTime]を編集する共通コンポーネント */
@Composable
fun RenderItemDisplayTimeEditComponent(
    displayTime: RenderData.DisplayTime,
    onUpdate: (RenderData.DisplayTime) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        OutlinedLongTextField(
            modifier = Modifier.weight(1f),
            value = displayTime.startMs,
            onValueChange = { startMs -> onUpdate(displayTime.copy(startMs = startMs)) },
            label = { Text(text = "表示開始時間（ミリ秒）") }
        )
        OutlinedLongTextField(
            modifier = Modifier.weight(1f),
            value = displayTime.durationMs,
            onValueChange = { durationMs -> onUpdate(displayTime.setDuration(durationMs)) },
            label = { Text(text = "表示時間（ミリ秒）") }
        )
    }
}