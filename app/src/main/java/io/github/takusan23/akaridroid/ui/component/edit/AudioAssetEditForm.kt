package io.github.takusan23.akaridroid.ui.component.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.data.AudioAssetData
import io.github.takusan23.akaridroid.ui.component.InitValueTextField

/**
 * 音声の編集フォーム
 *
 * @param modifier [Modifier]
 * @param audioAssetData 音声データクラス
 * @param onUpdate 更新時
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioAssetEditForm(
    modifier: Modifier = Modifier,
    audioAssetData: AudioAssetData,
    onUpdate: (AudioAssetData) -> Unit,
) {
    Column(modifier = modifier) {
        InitValueTextField(
            modifier = Modifier.padding(10.dp),
            initValue = audioAssetData.volume.toString(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onValueChange = { value ->
                value.toFloatOrNull()?.also { onUpdate(audioAssetData.copy(volume = it)) }
            },
            label = { Text(text = "音声のボリューム 0から1まで") }
        )
    }
}