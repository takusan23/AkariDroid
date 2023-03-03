package io.github.takusan23.akaridroid.timeline

import kotlinx.serialization.Serializable

/**
 * タイムライン上のアイテム
 * 仮
 *
 * @param id 識別するために使われる
 * @param xPos X座標
 * @param yPos Y座標
 * @param startMs 描画開始時間（ミリ秒）
 * @param endMs 描画終了時間（ミリ秒）
 * @param timelineItemType タイムライン上のアイテムの種類
 */
@Serializable
data class TimelineItemData(
    val id: Long = System.currentTimeMillis(),
    val xPos: Float,
    val yPos: Float,
    val startMs: Long,
    val endMs: Long,
    val timelineItemType: TimelineItemType
)

val TimelineItemData.timeRange: LongRange
    get() = startMs..endMs

val TimelineItemData.durationMs: Long
    get() = endMs - startMs