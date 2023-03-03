package io.github.takusan23.akaridroid.timeline

/**
 * タイムラインを分割した際のデータ
 *
 * @param offsetTime オフセット。0秒からの経過時間
 * @param timelineItemDataList タイムラインのアイテム
 */
data class TimelineChunkData(
    val offsetTime: Long = 0L,
    val timelineItemDataList: List<TimelineItemData> = emptyList()
)