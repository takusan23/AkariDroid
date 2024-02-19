package io.github.takusan23.akaridroid.v2

import kotlinx.serialization.Serializable

/**
 * 動画の素材
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
    val canvasRenderItem: List<CanvasItem>,
    val audioRenderItem: List<AudioItem>
) {

    /** 映像、音声で共通している */
    sealed interface RenderItem {
        /** アイテムを識別する一意の値 */
        val id: Long

        /** 時間 */
        val displayTime: DisplayTime
    }

    /** 保存先。ファイルパスか android の Uri */
    sealed interface FilePath {

        /** Uri 。シリアライズできるように String です。 */
        @Serializable
        data class Uri(val uriPath: String) : FilePath

        /** ファイルパス */
        @Serializable
        data class File(val filePath: String) : FilePath
    }

    /** Canvas に書く */
    sealed interface CanvasItem : RenderItem {

        /** 描画する位置 */
        val position: Position

        /** テキスト */
        @Serializable
        data class Text(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            val text: String,
            val textSize: Float = 24f,
            val fontColor: String = "#ffffff"
        ) : CanvasItem

        /** 画像 */
        @Serializable
        data class Image(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            val filePath: FilePath,
            val size: Size? = null
        ) : CanvasItem

        /** 動画（映像トラック） */
        @Serializable
        data class Video(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            val filePath: FilePath,
            val size: Size? = null,
            val cropTime: TimeCrop? = null,
            val chromaKeyColor: Int? = null
        ) : CanvasItem
    }

    /** 音声 */
    sealed interface AudioItem : RenderItem {

        /** 音声素材と、動画（音声トラック） */
        @Serializable
        data class Audio(
            override val id: Long = System.currentTimeMillis(),
            override val displayTime: DisplayTime,
            val filePath: FilePath,
            val cropTime: TimeCrop? = null,
            val volume: Float = DEFAULT_VOLUME
        ) : AudioItem

        companion object {

            /** [Audio.volume]の省略時の値 */
            const val DEFAULT_VOLUME = 1f
        }
    }


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
        override val start: Long
            get() = startMs
        override val endInclusive: Long
            get() = stopMs
    }

    /**
     * カットできる素材の場合（一部分のみを使う）
     * [io.github.takusan23.akaridroid.v2.canvasrender.RenderData.RenderItem.Video]と[AudioItem]くらい？
     */
    @Serializable
    data class TimeCrop(
        val cropStartMs: Long,
        val cropStopMs: Long
    ) : ClosedRange<Long> {
        // ClosedRange<Long> を実装することで、 in が使えるようになる
        override val start: Long
            get() = cropStartMs
        override val endInclusive: Long
            get() = cropStopMs
    }
}