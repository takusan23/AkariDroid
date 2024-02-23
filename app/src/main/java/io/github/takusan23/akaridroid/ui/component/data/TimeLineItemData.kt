package io.github.takusan23.akaridroid.ui.component.data

data class TimeLineItemData(
    val id: Long = System.currentTimeMillis(),
    val laneIndex: Int,
    val startMs: Long,
    val stopMs: Long
)

/** 表示時間の範囲を[LongRange]にする */
val TimeLineItemData.timeRange: LongRange
    get() = this.startMs until this.stopMs

/** 表示時間を時間にする */
val TimeLineItemData.durationMs: Long
    get() = this.stopMs - this.startMs