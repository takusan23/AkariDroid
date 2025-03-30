package io.github.takusan23.akaridroid.canvasrender

import io.github.takusan23.akaridroid.RenderData

/**
 * 映像トラックを描画するシステムを初期化するためのパラメータ
 *
 * @param outputWidth 映像の幅
 * @param outputHeight 映像の高さ
 * @param colorSpace 色空間。HDR か SDR か。
 */
data class VideoTrackRendererPrepareData(
    val outputWidth: Int,
    val outputHeight: Int,
    val colorSpace: RenderData.ColorSpace
)