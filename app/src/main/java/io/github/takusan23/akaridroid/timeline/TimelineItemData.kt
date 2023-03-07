package io.github.takusan23.akaridroid.timeline

import kotlinx.serialization.Serializable

/** タイムライン上のアイテム */
sealed interface TimelineItemData {

    /** 各タイムラインのアイテムを識別するために使われる */
    val id: Long

    /** タイムラインのアイテム開始時間（ミリ秒） */
    val startMs: Long

    /** タイムラインのアイテム終了時間（ミリ秒） */
    val endMs: Long

    /**
     * Canvas で描画可能なタイムラインのアイテム
     *
     * @param id 識別するために使われる
     * @param xPos X座標
     * @param yPos Y座標
     * @param startMs 描画開始時間（ミリ秒）
     * @param endMs 描画終了時間（ミリ秒）
     * @param timelineDrawItemType タイムライン上のアイテムの種類
     */
    @Serializable
    data class CanvasData(
        override val id: Long = System.currentTimeMillis(),
        override val startMs: Long,
        override val endMs: Long,
        val xPos: Float,
        val yPos: Float,
        val timelineDrawItemType: TimelineDrawItemType
    ) : TimelineItemData

    /**
     * 動画を描画する
     * akari-core では動画は別処理なので
     *
     * @param id 識別するために使われる
     * @param startMs 開始位置（ミリ秒）
     * @param endMs 終了位置（ミリ秒）
     * @param videoFilePath 動画パス
     * @param videoCutStartMs 動画を切り取る場合の位置（ミリ秒）
     * @param videoCutEndMs 動画を切り取る場合の位置（ミリ秒）
     */
    @Serializable
    data class VideoData(
        override val id: Long = System.currentTimeMillis(),
        override val startMs: Long,
        override val endMs: Long,
        val videoFilePath: String,
        val videoCutStartMs: Long? = null,
        val videoCutEndMs: Long? = null,
    ) : TimelineItemData {

        /** 切り取り範囲を LongRange にする */
        val videoCutRange: LongRange?
            get() = if (videoCutStartMs != null && videoCutEndMs != null) {
                videoCutStartMs..videoCutEndMs
            } else null
    }

    /**
     * 音声素材
     *
     * @param id 識別するために使われる
     * @param startMs 開始位置（ミリ秒）
     * @param endMs 終了位置（ミリ秒）
     * @param videoFilePath 動画パス
     * @param videoCutStartMs 動画を切り取る場合の位置（ミリ秒）
     * @param videoCutEndMs 動画を切り取る場合の位置（ミリ秒）
     */
    @Serializable
    data class AudioData(
        override val id: Long = System.currentTimeMillis(),
        override val startMs: Long,
        override val endMs: Long,
        val audioFilePath: String,
        val audioCutStartMs: Long? = null,
        val audioCutEndMs: Long? = null,
    ) : TimelineItemData {

        /** 切り取り範囲を LongRange にする */
        val audioCutRange: LongRange?
            get() = if (audioCutStartMs != null && audioCutEndMs != null) {
                audioCutStartMs..audioCutEndMs
            } else null

    }

    /** 開始、終了の LongRange。シリアライズの都合で Range はできない */
    val timeRange: LongRange
        get() = startMs..endMs

    /** アイテムの描画時間 */
    val durationMs: Long
        get() = endMs - startMs

}