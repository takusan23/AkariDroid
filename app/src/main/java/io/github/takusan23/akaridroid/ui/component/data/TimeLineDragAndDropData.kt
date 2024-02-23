package io.github.takusan23.akaridroid.ui.component.data

import androidx.compose.ui.unit.IntRect

data class TimeLineDragAndDropData(
    val timeLineItemData: TimeLineItemData,
    val dragStartRect: IntRect,
    val dragStopRect: IntRect
)