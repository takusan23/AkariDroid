package io.github.takusan23.akaridroid.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class CornerData(val topStart: Float, val bottomStart: Float, val topEnd: Float, val bottomEnd: Float)

private val StartRounded = CornerData(topStart = 50f, bottomStart = 50f, topEnd = 10f, bottomEnd = 10f)
private val EndRounded = CornerData(bottomEnd = 50f, topEnd = 50f, topStart = 10f, bottomStart = 10f)
private val SelectedRounded = CornerData(topStart = 50f, bottomStart = 50f, bottomEnd = 50f, topEnd = 50f)
private val DefaultRounded = CornerData(topStart = 10f, bottomStart = 10f, bottomEnd = 10f, topEnd = 10f)

/** ExpressiveButton の Shape に渡す。アニメーションされる */
@Composable
fun expressiveButtonShape(index: Int, size: Int, selected: Boolean): RoundedCornerShape {
    val after = when {
        selected -> SelectedRounded
        index == 0 -> StartRounded
        index == (size - 1) -> EndRounded
        else -> DefaultRounded
    }
    // アニメーションさせる
    val topStart = animateFloatAsState(after.topStart)
    val bottomEnd = animateFloatAsState(after.bottomEnd)
    val topEnd = animateFloatAsState(after.topEnd)
    val bottomStart = animateFloatAsState(after.bottomStart)
    return RoundedCornerShape(
        topStart = topStart.value,
        topEnd = topEnd.value,
        bottomEnd = bottomEnd.value,
        bottomStart = bottomStart.value,
    )
}

/** ExpressiveButton の親、真ん中そろえにするくらいしかないけど */
@Composable
fun ExpressiveButtonParent(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        content = content
    )
}

/** ButtonGroup 相当のボタン。洗濯すると色が変わって角丸になる */
@Composable
fun ExpressiveButton(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    if (selected) {
        Button(
            modifier = modifier,
            shape = shape,
            onClick = onClick,
            content = content
        )
    } else {
        OutlinedButton(
            modifier = modifier,
            shape = shape,
            onClick = onClick,
            content = content
        )
    }
}