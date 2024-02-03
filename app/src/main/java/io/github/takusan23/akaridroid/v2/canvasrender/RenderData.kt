package io.github.takusan23.akaridroid.v2.canvasrender

import kotlinx.serialization.Serializable

/**
 * Bitmap に描画するために必要なデータ
 *
 * @param durationMs トータル時間
 * @param videoSize 動画の縦横
 * @param canvasRenderItem 描画するアイテム
 * @param audioRenderItem 音声データ
 */
@Serializable
data class RenderData(
    val durationMs: Long,
    val videoSize: Size,
    val canvasRenderItem: List<RenderItem>,
    val audioRenderItem: List<RenderItem>
) {

    sealed interface RenderItem {

        /** アイテムを識別する一意の値 */
        val id: Long

        /** 描画する位置 */
        val position: Position

        /** 時間 */
        val displayTime: DisplayTime

        /** テキスト */
        @Serializable
        data class Text(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            val text: String,
            val textSize: Float? = null,
            val fontColor: Int? = null
        ) : RenderItem

        /** 画像 */
        @Serializable
        data class Image(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            val filePath: String,
            val size: Size? = null
        ) : RenderItem

        /** 動画 */
        @Serializable
        data class Video(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            val filePath: String,
            val size: Size? = null,
            val cropTimeCrop: TimeCrop? = null,
            val chromaKeyColor: Int? = null
        ) : RenderItem
    }

    /** 音声素材 */
    @Serializable
    data class AudioItem(
        val id: Long = System.currentTimeMillis(),
        val displayTime: DisplayTime,
        val cropTimeCrop: TimeCrop? = null
    )

    /**
     * 位置
     *
     * @param x X座標
     * @param y Y座標
     */
    @Serializable
    data class Position(
        val x: Float,
        val y: Float
    )

    /**
     * 大きさ
     *
     * @param width 幅
     * @param height 高さ
     */
    @Serializable
    data class Size(
        val width: Int,
        val height: Int
    )

    /**
     * 開始、終了を表す
     *
     * @param startMs 開始時間、ミリ秒
     * @param stopMs 終了時間、ミリ秒
     */
    @Serializable
    data class DisplayTime(
        val startMs: Long,
        val stopMs: Long
    ) : ClosedRange<Long> {
        // ClosedRange<Long> を実装することで、 in が使えるようになる
        override val endInclusive: Long
            get() = startMs
        override val start: Long
            get() = startMs
    }

    /**
     * カットできる素材の場合（一部分のみを使う）
     * [io.github.takusan23.akaridroid.v2.canvasrender.RenderData.RenderItem.Video]と[AudioItem]くらい？
     */
    @Serializable
    data class TimeCrop(
        val cropStartMs: Long,
        val cropStopMs: Long
    )
}