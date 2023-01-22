package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * 初期値を受け付けて内部で値を持つ
 *
 * @param modifier [Modifier]
 * @param initValue 初期値
 * @param onValueChange テキスト変化時によばれる
 * @param keyboardOptions IME設定
 * @param label ラベル
 */
@ExperimentalMaterial3Api
@Composable
fun InitValueTextField(
    modifier: Modifier = Modifier,
    initValue: String,
    onValueChange: (value: String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    label: @Composable () -> Unit = {}
) {
    val currentValue = remember { mutableStateOf(initValue) }
    OutlinedTextField(
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        value = currentValue.value,
        label = label,
        onValueChange = {
            currentValue.value = it
            onValueChange(it)
        }
    )
}