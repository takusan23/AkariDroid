package io.github.takusan23.akaridroid.ui.component.data

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val RoundedListTopShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 5.dp, bottomEnd = 5.dp)
val RoundedListInnerShape = RoundedCornerShape(size = 5.dp)
val RoundedListEndShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 20.dp, bottomEnd = 20.dp)

fun getRoundedShape(size: Int, index: Int) = when {
    index == 0 -> RoundedListTopShape
    index == (size - 1) -> RoundedListEndShape
    else -> RoundedListInnerShape
}