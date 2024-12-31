package io.github.takusan23.akaridroid.canvasrender

/**
 * 映像トラックを描画するシステムを初期化するためのパラメータ
 *
 * @param outputWidth 映像の幅
 * @param outputHeight 映像の高さ
 * @param isEnableTenBitHdr 10-bit HDR を有効にするか
 */
data class VideoTrackRendererPrepareData(
    val outputWidth: Int,
    val outputHeight: Int,
    val isEnableTenBitHdr: Boolean = false
)