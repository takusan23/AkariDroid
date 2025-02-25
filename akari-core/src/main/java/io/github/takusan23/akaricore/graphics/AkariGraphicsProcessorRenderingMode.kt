package io.github.takusan23.akaricore.graphics

import android.view.Surface

/** [AkariGraphicsTextureRenderer] の描画モード */
sealed interface AkariGraphicsProcessorRenderingMode {

    /**
     * Surface にレンダリングする。
     * 画面に表示する SurfaceView、動画に保存する MediaCodec / MediaRecorder など。
     */
    data class SurfaceRendering(val surface: Surface) : AkariGraphicsProcessorRenderingMode

    /**
     * オフスクリーンレンダリング用。
     * Surface が無いが、[AkariGraphicsProcessor] を利用したい場合。
     *
     * @param width 横
     * @param height 縦
     */
    data class OffscreenRendering(val width: Int, val height: Int) : AkariGraphicsProcessorRenderingMode
}