package io.github.takusan23.akaridroid.tool.data

/** [io.github.takusan23.akaridroid.v2.tool.UriTool.analyzeImage]とかの返り値 */
sealed interface AnalyzeResult {

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
     * 画像
     *
     * @param size サイズ
     */
    data class Image(
        val size: Size
    ) : AnalyzeResult

    /**
     * 音声
     *
     * @param durationMs 長さ
     */
    data class Audio(
        val durationMs: Long
    ) : AnalyzeResult

    /**
     * 動画
     *
     * @param size サイズ
     * @param durationMs 長さ
     * @param hasAudioTrack 音声トラックがあれば true
     */
    data class Video(
        val size: Size,
        val durationMs: Long,
        val hasAudioTrack: Boolean
    ) : AnalyzeResult
}