package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.takusan23.akaridroid.R

/** 押したときの増加量 */
private const val ROTATE_BUTTON_CLICK_STEP = 90

/**
 * 回転の編集をするコンポーネント
 *
 * @param modifier [Modifier]
 * @param rotation 今の回転度
 * @param onUpdate 値変更時
 */
@Composable
fun RenderItemRotationEditComponent(
    modifier: Modifier = Modifier,
    rotation: Int,
    onUpdate: (rotation: Int) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.edit_renderitem_rotation_title)
        )
        // TODO force update のアンチパターンを辞める
        key(rotation) {
            OutlinedIntTextField(
                modifier = Modifier.weight(1f),
                value = rotation,
                onValueChange = onUpdate,
                label = { Text(text = stringResource(id = R.string.edit_renderitem_rotation_degree)) }
            )
        }
        IconButton(
            onClick = {
                var after = rotation + ROTATE_BUTTON_CLICK_STEP
                if (360 < after) {
                    after = 0
                }
                onUpdate(after)
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_rotate_90_degrees_cw_24px),
                contentDescription = null
            )
        }
    }
}

