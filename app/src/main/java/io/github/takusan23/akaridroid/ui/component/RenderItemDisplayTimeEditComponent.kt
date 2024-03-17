package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.takusan23.akaridroid.RenderData

/** [RenderData.DisplayTime]を編集する共通コンポーネント */
@Composable
fun RenderItemDisplayTimeEditComponent(
    displayTime: RenderData.DisplayTime,
    onUpdate: (RenderData.DisplayTime) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = "表示開始時間"
            )
            DurationInput(
                modifier = Modifier.weight(2f),
                durationMs = displayTime.startMs,
                onChange = { startMs -> onUpdate(displayTime.copy(startMs = startMs)) }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = "表示時間"
            )
            DurationInput(
                modifier = Modifier.weight(2f),
                durationMs = displayTime.durationMs,
                onChange = { durationMs -> onUpdate(displayTime.setDuration(durationMs)) }
            )
        }
    }
}