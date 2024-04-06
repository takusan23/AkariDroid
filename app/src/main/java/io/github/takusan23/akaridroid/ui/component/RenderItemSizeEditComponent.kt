package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData

/** [RenderData.Size]を編集する共通コンポーネント */
@Composable
fun RenderItemSizeEditComponent(
    size: RenderData.Size,
    onUpdate: (RenderData.Size) -> Unit
) {
    val initialValue = remember { mutableStateOf(size) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        OutlinedIntTextField(
            modifier = Modifier.weight(1f),
            value = size.width,
            onValueChange = { width -> onUpdate(size.copy(width = width)) },
            label = { Text(text = stringResource(id = R.string.edit_renderitem_size_width)) }
        )
        OutlinedIntTextField(
            modifier = Modifier.weight(1f),
            value = size.height,
            onValueChange = { height -> onUpdate(size.copy(height = height)) },
            label = { Text(text = stringResource(id = R.string.edit_renderitem_size_height)) }
        )
        // リセット
        IconButton(onClick = { onUpdate(initialValue.value) }) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_reset_wrench_24px), contentDescription = null)
        }
    }
}