package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * 音量調整とか。Float の値を変更するやつ
 *
 * @param modifier [Modifier]
 * @param label 文言
 * @param iconResId アイコン
 * @param value 値
 * @param onChange 値変化時
 * @param valueRange スライダーの範囲
 */
@Composable
fun RenderItemFloatEditComponent(
    modifier: Modifier = Modifier,
    label: String,
    iconResId: Int,
    value: Float,
    onChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Row(
        modifier = modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null
        )

        Text(
            modifier = Modifier,
            text = label
        )

        Slider(
            modifier = Modifier.weight(1f),
            value = value,
            onValueChange = onChange,
            valueRange = valueRange
        )

        // 小数点2桁まで
        Text(text = "%.2f".format(((value * 100).toInt() / 100f)))
    }
}