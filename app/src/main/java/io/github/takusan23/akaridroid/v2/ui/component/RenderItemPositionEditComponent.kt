package io.github.takusan23.akaridroid.v2.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.takusan23.akaridroid.v2.RenderData

/** [RenderData.CanvasItem.position]を編集する共通コンポーネント */
@Composable
fun RenderItemPositionEditComponent(
    position: RenderData.Position,
    onUpdate: (RenderData.Position) -> Unit
) {
    Row {
        OutlinedFloatTextField(
            modifier = Modifier.weight(1f),
            value = position.x,
            onValueChange = { x -> onUpdate(position.copy(x = x)) },
            label = { Text(text = "X 座標") }
        )
        OutlinedFloatTextField(
            modifier = Modifier.weight(1f),
            value = position.y,
            onValueChange = { y -> onUpdate(position.copy(x = y)) },
            label = { Text(text = "Y 座標") }
        )
    }
}