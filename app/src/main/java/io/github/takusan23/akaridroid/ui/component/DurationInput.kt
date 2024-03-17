package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 時、分、秒、ミリ秒 をパースして持っておくデータクラス。
 * 60_000 をパースしたら minute=1 になる。
 *
 * @param hour 時間
 * @param minute 分
 * @param seconds 秒
 * @param milliSeconds ミリ秒
 */
private data class DurationUnitParameters(
    val hour: Int,
    val minute: Int,
    val seconds: Int,
    val milliSeconds: Int
) {

    /** ミリ秒に変換する */
    fun toMilliSeconds(): Long {
        val hourMs = hour * 60 * 60 * 1000L
        val minuteMs = minute * 60 * 1000L
        val secondsMs = seconds * 1000L
        return hourMs + minuteMs + secondsMs + milliSeconds
    }

    companion object {

        /** ミリ秒から作る */
        fun fromMilliseconds(durationMs: Long): DurationUnitParameters {
            val hour = (durationMs / 1000 / 3600).toInt()
            val minute = (durationMs / 1000 / 60 % 60).toInt()
            val seconds = (durationMs / 1000 % 60).toInt()
            val milliSeconds = (durationMs % 1000).toInt()

            return DurationUnitParameters(
                hour = hour,
                minute = minute,
                seconds = seconds,
                milliSeconds = milliSeconds
            )
        }
    }
}

/**
 * 時間を入力する。
 * 時、分、秒、ミリ秒　それぞれテキストフィールドがある。
 *
 * @param modifier [Modifier]
 * @param durationMs 時間
 * @param onChange 変化時
 */
@Composable
fun DurationInput(
    modifier: Modifier = Modifier,
    durationMs: Long,
    onChange: (Long) -> Unit
) {
    // 時、分、秒にパースしておく
    val durationParameters = remember(durationMs) { DurationUnitParameters.fromMilliseconds(durationMs) }

    /** [onChange]を呼ぶ */
    fun update(
        hour: Int = durationParameters.hour,
        minute: Int = durationParameters.minute,
        seconds: Int = durationParameters.seconds,
        milliSeconds: Int = durationParameters.milliSeconds
    ) {
        val toMilliSeconds = durationParameters.copy(
            hour = hour,
            minute = minute,
            seconds = seconds,
            milliSeconds = milliSeconds
        ).toMilliSeconds()
        onChange(toMilliSeconds)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        DurationUnitInput(
            modifier = Modifier.weight(1f),
            value = durationParameters.hour.toString().padStart(2, '0'),
            onValueChange = { hour -> hour.toIntOrNull()?.also { hourInt -> update(hour = hourInt) } },
            label = "時",
            maxLength = 2
        )

        Text(
            text = ":",
            fontSize = 20.sp
        )

        DurationUnitInput(
            modifier = Modifier.weight(1f),
            value = durationParameters.minute.toString().padStart(2, '0'),
            onValueChange = { minute -> minute.toIntOrNull()?.also { minuteInt -> update(minute = minuteInt) } },
            label = "分",
            maxLength = 2
        )

        Text(
            text = ":",
            fontSize = 20.sp
        )

        DurationUnitInput(
            modifier = Modifier.weight(1f),
            value = durationParameters.seconds.toString().padStart(2, '0'),
            onValueChange = { seconds -> seconds.toIntOrNull()?.also { secondsInt -> update(seconds = secondsInt) } },
            label = "秒",
            maxLength = 2
        )

        Text(
            text = ".",
            fontSize = 20.sp
        )

        DurationUnitInput(
            modifier = Modifier.weight(1f),
            value = durationParameters.milliSeconds.toString().padStart(3, '0'),
            onValueChange = { ms -> ms.toIntOrNull()?.also { msInt -> update(milliSeconds = msInt) } },
            label = "ミリ秒",
            maxLength = 3
        )
    }
}

@Composable
private fun DurationUnitInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    label: String
) {
    val inputValue = remember { mutableStateOf(value) }

    OutlinedTextField(
        modifier = modifier,
        label = {
            Text(
                text = label,
                maxLines = 1
            )
        },
        value = inputValue.value,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(textAlign = TextAlign.Center),
        isError = maxLength != inputValue.value.length,
        singleLine = true,
        onValueChange = {
            inputValue.value = it
            // 桁を超えないように。超えたら onChange の通知しない
            if (inputValue.value.length <= maxLength) {
                onValueChange(inputValue.value.take(maxLength))
            }
        }
    )

}