package io.github.takusan23.akaridroid.timeline

import kotlinx.serialization.Serializable

/**
 * タイムラインの情報（素材を並べるやつ）
 *
 * @param videoDurationMs 動画の合計時間
 * @param timelineItemDataList タイムラインのアイテム
 */
@Serializable
data class TimelineData(
    val videoDurationMs: Long = 10_000L,
    val timelineItemDataList: List<TimelineItemData> = emptyList()
) {

    /**
     * タイムラインを動画の有無で分割する。
     * 各アイテムにタイムラインのアイテムの配列が入る。
     * ```
     * [動画無し区間,動画1あり区間,動画2あり区間,動画なし区間]
     * ```
     */
    fun getTimelineChunkList(): List<TimelineChunkData> {
        // 動画がある
        val videoItemOnlyList = timelineItemDataList.filter { it.timelineItemType is TimelineItemType.VideoItem }
        if (videoItemOnlyList.isEmpty()) {
            // すべて Canvas で描画可能
            // そのまま返す
            return listOf(TimelineChunkData(0, videoDurationMs, timelineItemDataList))
        }
        // return するリスト
        val chunkList = mutableListOf<TimelineChunkData>()

        // filterに渡す関数
        fun filterFunc(it: TimelineItemData, timeRange: LongRange): Boolean {
            val value = timeRange.last - timeRange.first
            return if (it.durationMs <= value) {
                // 範囲内
                (it.startMs in timeRange && it.endMs in timeRange)
            } else {
                // Canvas / 動画アイテム よりも大きい場合はどちらかが被っていれば
                (timeRange.first in it.timeRange || timeRange.last in it.timeRange)
            }
        }

        var nextStartMs = 0L
        videoItemOnlyList.forEach { item ->
            // 動画よりも前に Canvas がある場合
            val videoStartMs = item.startMs
            val videoEndMs = item.endMs

            val diffVideoStartMs = videoStartMs - nextStartMs
            if (diffVideoStartMs > 0) {
                val range = nextStartMs..videoStartMs
                // 動画の前にあるタイムラインアイテム
                chunkList += TimelineChunkData(
                    startMs = range.first,
                    endMs = range.last,
                    timelineItemDataList = timelineItemDataList.filter { filterFunc(it, range) }
                )
            }
            // 動画と重なる
            val range = item.timeRange
            chunkList += TimelineChunkData(
                startMs = range.first,
                endMs = range.last,
                timelineItemDataList = timelineItemDataList.filter { filterFunc(it, range) }
            )
            // 次の開始地点
            nextStartMs = videoEndMs
        }
        // 最後
        if (nextStartMs < videoDurationMs) {
            val range = nextStartMs..videoDurationMs
            chunkList += TimelineChunkData(
                startMs = range.first,
                endMs = range.last,
                timelineItemDataList = timelineItemDataList.filter { filterFunc(it, range) }
            )
        }

        return chunkList
    }

}