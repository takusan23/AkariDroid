package io.github.takusan23.akaridroid.v2.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/** [OutlinedTextField]の[Float]版 */
@Composable
fun OutlinedFloatTextField(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    label: @Composable (() -> Unit)? = null
) {
    val numberText = remember { mutableStateOf(value.toString()) }
    OutlinedTextField(
        modifier = modifier,
        value = numberText.value,
        onValueChange = {
            numberText.value = it
            it.toFloatOrNull()?.also { float ->
                onValueChange(float)
            }
        },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

/** [OutlinedTextField]の[Int]版 */
@Composable
fun OutlinedIntTextField(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    label: @Composable (() -> Unit)? = null
) {
    val numberText = remember { mutableStateOf(value.toString()) }
    OutlinedTextField(
        modifier = modifier,
        value = numberText.value,
        onValueChange = {
            numberText.value = it
            it.toIntOrNull()?.also { int ->
                onValueChange(int)
            }
        },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

/** [OutlinedTextField]の[Long]版 */
@Composable
fun OutlinedLongTextField(
    modifier: Modifier = Modifier,
    value: Long,
    onValueChange: (Long) -> Unit,
    label: @Composable (() -> Unit)? = null
) {
    val numberText = remember { mutableStateOf(value.toString()) }
    OutlinedTextField(
        modifier = modifier,
        value = numberText.value,
        onValueChange = {
            numberText.value = it
            it.toLongOrNull()?.also { long ->
                onValueChange(long)
            }
        },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}
