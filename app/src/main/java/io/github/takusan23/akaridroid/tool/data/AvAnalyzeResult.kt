package io.github.takusan23.akaridroid.tool.data

/** [io.github.takusan23.akaridroid.tool.AvAnalyze]とかの返り値 */
sealed interface AvAnalyzeResult {

    /**
     * [Image]、[Video]の縦横サイズ
     *
     * @param width 幅
     * @param height 高さ
     */
    data class Size(
        val width: Int,
        val height: Int
    )

    /**
     * 10Bit HDR 動画の場合は、色域とガンマカーブ。
     * HLG の場合は BT.2020 / HLG になると思う。
     *
     * @param colorStandard 色域
     * @param colorTransfer ガンマカーブ
     */
    data class TenBitHdrInfo(
        val colorStandard: Int,
        val colorTransfer: Int
    )

    /**
     * 画像
     *
     * @param size サイズ
     */
    data class Image(
        val size: Size
    ) : AvAnalyzeResult

    /**
     * 音声
     *
     * @param durationMs 長さ
     */
    data class Audio(
        val durationMs: Long
    ) : AvAnalyzeResult

    /**
     * 動画
     *
     * @param size サイズ
     * @param durationMs 長さ
     * @param hasAudioTrack 音声トラックがあれば true
     * @param tenBitHdrInfoOrSdrNull SDR 動画の場合は null。10Bit HDR 動画の場合は色域とガンマカーブ
     */
    data class Video(
        val size: Size,
        val durationMs: Long,
        val hasAudioTrack: Boolean,
        val tenBitHdrInfoOrSdrNull: TenBitHdrInfo?
    ) : AvAnalyzeResult
}
