package io.github.takusan23.akaridroid.ui.component.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.data.AudioAssetData

/**
 * 音声の編集フォーム
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioAssetEditForm(
    modifier: Modifier = Modifier,
    audioAssetData: AudioAssetData,
    onUpdate: (AudioAssetData) -> Unit,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier.padding(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = audioAssetData.volume.toString(),
            onValueChange = { onUpdate(audioAssetData.copy(volume = it.toFloat())) },
            label = { Text(text = "音声のボリューム 0から1まで") }
        )
    }
}