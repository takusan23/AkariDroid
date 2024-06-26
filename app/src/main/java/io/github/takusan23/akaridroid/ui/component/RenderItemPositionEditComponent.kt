package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData

/** [RenderData.CanvasItem.position]を編集する共通コンポーネント */
@Composable
fun RenderItemPositionEditComponent(
    position: RenderData.Position,
    onUpdate: (RenderData.Position) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        OutlinedFloatTextField(
            modifier = Modifier.weight(1f),
            value = position.x,
            onValueChange = { x -> onUpdate(position.copy(x = x)) },
            label = { Text(text = stringResource(id = R.string.edit_renderitem_position_x)) }
        )
        OutlinedFloatTextField(
            modifier = Modifier.weight(1f),
            value = position.y,
            onValueChange = { y -> onUpdate(position.copy(y = y)) },
            label = { Text(text = stringResource(id = R.string.edit_renderitem_position_y)) }
        )
    }
}