package io.github.takusan23.akaridroid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 動画の素材
 *
 * @param version [RenderData]のバージョン。破壊的変更があればこれを見る
 * @param durationMs トータル時間
 * @param videoSize 動画の縦横
 * @param isEnableTenBitHdr 10-bit HDR の編集を有効にする場合。現状 HLG のみなので bool
 * @param canvasRenderItem 描画するアイテム
 * @param audioRenderItem 音声データ
 */
@Serializable
data class RenderData(
    val version: Int = VERSION,
    val durationMs: Long = 60_000L,
    val videoSize: Size = Size(1280, 720),
    val isEnableTenBitHdr: Boolean = false,
    val canvasRenderItem: List<CanvasItem> = emptyList(),
    val audioRenderItem: List<AudioItem> = emptyList()
) {

    /** 映像、音声で共通している */
    @Serializable
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
    @Serializable
    sealed interface FilePath {

        /** Uri 。シリアライズできるように String です。 */
        @Serializable
        @SerialName("android_uri")
        data class Uri(val uriPath: String) : FilePath

        /** ファイルパス */
        @Serializable
        @SerialName("java_file")
        data class File(val filePath: String) : FilePath
    }

    /** Canvas に書く */
    @Serializable
    sealed interface CanvasItem : RenderItem {

        /** 描画する位置 */
        val position: Position

        /** テキスト */
        @Serializable
        @SerialName("text") // sealed class を kotlinx/serialization 出来るように。
        data class Text(
            override val id: Long = System.currentTimeMillis(), // TODO UnixTime ぽいのを入れているが、全然時間以外のも入って来ていい
            override val position: Position,
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            val text: String,
            val textSize: Float = 50f,
            val fontColor: String = "#ffffff",
            val strokeColor: String? = null, // 枠取り文字にするなら
            val fontName: String? = null // フォントを使うなら、FontManager で追加したときの名前。フォントファイルが有るかは常に見る必要がある
        ) : CanvasItem

        /** 画像 */
        @Serializable
        @SerialName("image")
        data class Image(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            val filePath: FilePath, // TODO FilePath、動画、写真、音声で共通なので interface 切りたい
            val size: Size
        ) : CanvasItem

        /** 動画（映像トラック） */
        @Serializable
        @SerialName("video")
        data class Video(
            override val id: Long = System.currentTimeMillis(),
            override val position: Position,
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            val filePath: FilePath,
            val size: Size,
            val displayOffset: DisplayOffset = DisplayOffset(0),
            val chromaKeyColor: Int? = null,
            val dynamicRange: DynamicRange = DynamicRange.SDR,
            val rotation: Int = 0
        ) : CanvasItem {

            /** ダイナミックレンジ */
            @Serializable
            enum class DynamicRange {

                /** 従来の動画。SDR */
                @SerialName("sdr")
                SDR,

                /** HDR 動画。HLG 形式 */
                @SerialName("hdr_hlg")
                HDR_HLG
            }

        }

        /** 図形 */
        @Serializable
        @SerialName("shape")
        data class Shape(
            override val id: Long = System.currentTimeMillis(),
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            override val position: Position,
            val color: String = "#ffffff",
            val size: Size,
            val shapeType: ShapeType
        ) : CanvasItem {

            /** 図形タイプ */
            @Serializable
            enum class ShapeType {
                /** 四角形 */
                Rect,

                /** 丸 */
                Circle
            }

        }

        /** シェーダー */
        @Serializable
        @SerialName("shader")
        data class Shader(
            override val id: Long = System.currentTimeMillis(),
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            override val position: Position,
            val size: Size,
            val name: String, // タイムラインの名前表示に使ってる
            val fragmentShader: String
        ) : CanvasItem

        /** 切り替えアニメーション */
        @Serializable
        @SerialName("switch_animation")
        data class SwitchAnimation(
            override val id: Long = System.currentTimeMillis(),
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            override val position: Position,
            val size: Size,
            val animationType: SwitchAnimationType
        ) : CanvasItem {

            /** アニメーション一覧 */
            @Serializable
            enum class SwitchAnimationType {
                @SerialName("fade_in_out")
                FADE_IN_OUT,

                @SerialName("fade_in_out_white")
                FADE_IN_OUT_WHITE,

                @SerialName("slide")
                SLIDE,

                @SerialName("blur")
                BLUR
            }
        }

        /** エフェクト */
        @Serializable
        @SerialName("effect")
        data class Effect(
            override val id: Long = System.currentTimeMillis(),
            override val displayTime: DisplayTime,
            override val layerIndex: Int,
            override val position: Position,
            val size: Size,
            val effectType: EffectType
        ) : CanvasItem {

            /** エフェクト一覧 */
            @Serializable
            enum class EffectType {

                @SerialName("mosaic")
                MOSAIC,

                @SerialName("blur")
                BLUR,

                @SerialName("monochrome")
                MONOCHROME,

                @SerialName("threshold")
                THRESHOLD
            }
        }

    }

    /** 音声 */
    @Serializable
    sealed interface AudioItem : RenderItem {

        /** 音声素材と、動画（音声トラック） */
        @Serializable
        @SerialName("audio")
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
     * 素材の表示時間を表す
     *
     * アスキーアート
     * 以下がタイムラインだとして
     * 0----------10----------20----------30
     *
     * startMs=10 / durationMs=20 を追加したらこう
     * 0----------10----------20----------30
     * <----10--->[<----------20---------->]
     *
     * startMs=10 / durationMs=30 ならこうです
     * 0----------10----------20----------30
     * <----10--->[<----------------30-------------->]
     *
     * @param startMs 開始時間、ミリ秒
     * @param durationMs この時間から、何ミリ秒再生するか。[stopMs]の計算にも利用されます。再生速度は考慮しない値になります。**間違っても終了時間を入れるわけではありません。**
     * @param playbackSpeed 再生速度。動画と音声ではこれが利用されます
     */
    @Serializable
    data class DisplayTime(
        val startMs: Long,
        val durationMs: Long,
        val playbackSpeed: Float = DEFAULT_PLAYBACK_SPEED
    ) : ClosedRange<Long> {
        // ClosedRange<Long> を実装することで、 in が使えるようになる
        override val start: Long
            get() = startMs
        override val endInclusive: Long
            get() = stopMs

        /** **再生速度を考慮した**、終了時間を出す */
        val stopMs: Long
            get() = startMs + playbackSpeedDurationMs

        /** 再生速度を考慮した[durationMs] */
        val playbackSpeedDurationMs: Long
            get() = (durationMs / playbackSpeed).toLong()

        /**
         * 時間を足した（もしくは引いた）[DisplayTime]を作る。
         *
         * @param appendTimeMs [startMs]よりどれだけ時間を増加（もしくは減少）させるか
         * @return [DisplayTime]
         */
        fun appendTime(appendTimeMs: Long): DisplayTime = copy(startMs = startMs + appendTimeMs)

        /**
         * 指定した時間で[DisplayTime]を2つに分割する。
         * タイムライン上から呼び出される想定。
         *
         * 例：
         * startMs=10 / durationMs=40 をタイムライン 20ms の位置で分割する
         * 0----------10----------20----------30
         * <----10--->[<---------------------40------------------->]
         *
         * こんな感じ
         * 0----------10----------20----------30
         * <----10--->[<----10--->][<--------------30------------->]
         *
         * @param cutMs 分割する時間。再生速度は考慮しないで（例：10秒で前後期に切りたいけど、2倍速だから5秒を渡す。とかにはしないで良い）
         * @return Pair。first が前。second が後ろ
         */
        fun splitTime(cutMs: Long): Pair<DisplayTime, DisplayTime> {
            // RenderData.durationMs / startMs は再生速度を適用していません。
            // cutMs に再生速度を掛け算したり、しなかったりしています。この差は何かというと、
            // タイムライン上に表示されているアイテムはすでに再生速度に応じてタイムライン上のアイテムの長さが調整されている。（stopMs が再生速度を適用した値を返す）
            // 長さが調整されている状態でタイムラインを操作し、cutMs の位置で分割したい場合、startMs / durationMs は再生速度を適用していない値のため、cutMs（ユーザーが見ている再生速度を適用した位置）を操作する必要がある。
            // 詳しくはテスト見て。

            // 1つ目
            val displayTimeA = DisplayTime(
                startMs = this.startMs,// 分割した前側はそのまま
                durationMs = ((cutMs - this.startMs) * playbackSpeed).toLong(), // 開始位置から分割位置まで
                playbackSpeed = this.playbackSpeed
            )
            // 2つ目
            // 引き続き、startMs / durationMs は再生速度を適用しない値をいれる必要があります
            val displayTimeB = DisplayTime(
                startMs = cutMs, // 切り出す位置
                durationMs = this.durationMs - displayTimeA.durationMs, // 残りの時間
                playbackSpeed = this.playbackSpeed
            )
            return displayTimeA to displayTimeB
        }

        companion object {
            /** 再生速度 */
            const val DEFAULT_PLAYBACK_SPEED = 1f
        }
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
     * また、[DisplayTime.playbackSpeed]は適用済みだと考えて。
     * タイムラインで分割したあとに速度を変更しても、開始位置がズレないように。
     *
     * @param offsetFirstMs 最初からどれだけカットして利用するか
     */
    @Serializable
    data class DisplayOffset(val offsetFirstMs: Long)

    companion object {

        /** 現在のバージョン。破壊的変更があればこれで判定する */
        private const val VERSION = 1
    }
}