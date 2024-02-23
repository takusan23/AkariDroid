package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData

/** [RenderData.Size]を編集する共通コンポーネント */
// TODO サイズは nullable ではなく、動画や画像から実際のサイズを初期値に入れておくべき
@Composable
fun RenderItemSizeEditComponent(
    size: RenderData.Size?,
    onUpdate: (RenderData.Size?) -> Unit
) {
    val size = remember(size) { mutableStateOf(size) }

    if (size.value == null) {
        Button(onClick = { onUpdate(RenderData.Size(0, 0)) }) {
            Text(text = "サイズを指定する")
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            OutlinedIntTextField(
                modifier = Modifier.weight(1f),
                value = size.value!!.width,
                onValueChange = { width -> onUpdate(size.value!!.copy(width = width)) },
                label = { Text(text = "幅") }
            )
            OutlinedIntTextField(
                modifier = Modifier.weight(1f),
                value = size.value!!.height,
                onValueChange = { height -> onUpdate(size.value!!.copy(height = height)) },
                label = { Text(text = "高さ") }
            )
            // 削除
            IconButton(onClick = { onUpdate(null) }) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null)
            }
        }
    }
}