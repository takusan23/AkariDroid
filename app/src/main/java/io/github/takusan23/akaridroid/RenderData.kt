package io.github.takusan23.akaridroid

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

        /**
         * タイムラインのレーン番号。
         * [CanvasItem]の場合は重なり順になる。
         * 音声の場合は重なり順とか関係なく合成されるため、タイムラインの表示のためだけにある。ここに入れるのもなんかあれな気がするけど。
         */
        val layerIndex: Int
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
            override val id: Long = System.currentTimeMillis(), // TODO UnixTime ぽいのを入れているが、全然時間以外のも入って来ていい
            override val position: Position,
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            val text: String,
            val textSize: Float = 50f,
            val fontColor: String = "#ffffff"
        ) : CanvasItem

        /** 画像 */
        @Serializable
        data class Image(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            val filePath: FilePath,
            val size: Size
        ) : CanvasItem

        /** 動画（映像トラック） */
        @Serializable
        data class Video(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            val filePath: FilePath,
            val size: Size,
            val displayOffset: DisplayOffset = DisplayOffset(0),
            val chromaKeyColor: Int? = null
        ) : CanvasItem

        /** 図形 */
        @Serializable
        data class Shape(
            override val id: Long = System.currentTimeMillis(),
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            override val position: Position,
            val color: String = "#ffffff",
            val size: Size,
            val type: Type
        ) : CanvasItem {

            /** 図形タイプ */
            enum class Type {
                /** 四角形 */
                Rect,

                /** 丸 */
                Circle
            }

        }
    }

    /** 音声 */
    sealed interface AudioItem : RenderItem {

        /** 音声素材と、動画（音声トラック） */
        @Serializable
        data class Audio(
            override val id: Long = System.currentTimeMillis(),
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            val filePath: FilePath,
            val displayOffset: DisplayOffset = DisplayOffset(0),
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

        /** 時間を出す */
        val durationMs: Long
            get() = stopMs - start

        /**
         * 時間を足した（もしくは引いた）[DisplayTime]を作る。
         *
         * @param appendTimeMs [startMs]よりどれだけ時間を増加（もしくは減少）させるか
         * @return [DisplayTime]
         */
        fun appendTime(appendTimeMs: Long): DisplayTime = setTime(setTimeMs = startMs + appendTimeMs)

        /**
         * [startMs]の時間を[setTimeMs]にして、その分ずらす
         *
         * @param setTimeMs 開始位置。[startMs]になる
         * @return [DisplayTime]
         */
        fun setTime(setTimeMs: Long): DisplayTime = DisplayTime(
            startMs = setTimeMs,
            stopMs = setTimeMs + durationMs
        )

        /**
         * [startMs]から[durationMs]までの[DisplayTime]を作る。
         * [stopMs]を足す手間が減る。
         *
         * @param durationMs 表示時間。[DisplayTime.durationMs]です。
         * @return [DisplayTime]
         */
        fun setDuration(durationMs: Long): DisplayTime = DisplayTime(
            startMs = startMs,
            stopMs = startMs + durationMs
        )
    }

    /**
     * 再生位置のオフセット。
     * 動画トラックと、音声トラックは分割できて、途中の再生から使うとかあるので。その値。
     *
     * [DisplayTime]とは違う。
     * [DisplayTime]は表示すべき位置です。
     * [DisplayOffset]は[DisplayTime]で表示することになった際に、動画素材や音声素材の開始、終了位置の調整に使われます。
     *
     * [DisplayTime]で2秒表示するけど、表示する動画は5秒から始めたい。とかの時に使います。
     * なので、[DisplayTime.durationMs]分は存在している必要があります。それ以上[DisplayOffset.offsetFirstMs]等で削ってはいけない。
     *
     * @param offsetFirstMs 最初からどれだけカットして利用するか
     */
    @Serializable
    data class DisplayOffset(val offsetFirstMs: Long)
}