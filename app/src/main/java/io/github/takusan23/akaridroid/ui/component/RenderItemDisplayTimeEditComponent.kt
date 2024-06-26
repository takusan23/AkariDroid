package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData

/**
 * [RenderData.DisplayTime]を編集する共通コンポーネント
 *
 * @param isEditPlayback 再生速度調整を出すか。音声と映像は使うので true。
 */
@Composable
fun RenderItemDisplayTimeEditComponent(
    modifier: Modifier = Modifier,
    isEditPlayback: Boolean = false,
    displayTime: RenderData.DisplayTime,
    onUpdate: (RenderData.DisplayTime) -> Unit
) {
    Column(modifier) {

        // 再生速度の調整をする場合
        // 映像と音声のみ
        if (isEditPlayback) {
            RenderItemFloatEditComponent(
                label = stringResource(id = R.string.edit_renderitem_displaytime_playback_speed),
                iconResId = R.drawable.swap_driving_apps_wheel_24px,
                value = displayTime.playbackSpeed,
                onChange = { playbackSpeed -> onUpdate(displayTime.copy(playbackSpeed = playbackSpeed)) },
                valueRange = 0.1f..2f
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.edit_renderitem_displaytime_title)
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
                text = stringResource(id = R.string.edit_renderitem_displaytime_time)
            )
            DurationInput(
                modifier = Modifier.weight(2f),
                durationMs = displayTime.durationMs,
                onChange = { durationMs -> onUpdate(displayTime.copy(durationMs = durationMs)) }
            )
        }
    }
}