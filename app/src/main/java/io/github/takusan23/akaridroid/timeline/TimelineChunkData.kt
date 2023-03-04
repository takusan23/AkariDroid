package io.github.takusan23.akaridroid.timeline

/**
 * タイムラインを分割した際のデータ
 * 主にエンコードの際に分割する必要があり、そのために使われる
 *
 * @param startMs 開始位置
 * @param endMs 終了位置
 * @param timelineItemDataList タイムラインのアイテム
 */
data class TimelineChunkData(
    val startMs: Long,
    val endMs: Long,
    val timelineItemDataList: List<TimelineItemData> = emptyList()
)

/** チャンクの時間 */
val TimelineChunkData.durationMs: Long
    get() = endMs - startMs