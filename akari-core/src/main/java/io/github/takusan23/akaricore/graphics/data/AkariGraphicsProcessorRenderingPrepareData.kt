package io.github.takusan23.akaricore.graphics.data

import android.view.Surface

/** [io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer] の描画モード */
sealed interface AkariGraphicsProcessorRenderingPrepareData {

    /** 横。映像に合わせる。 */
    val width: Int

    /** 縦。映像に合わせる。 */
    val height: Int

    /**
     * Surface にレンダリングする。
     *
     * @param surface 画面に表示する SurfaceView、動画に保存する MediaCodec / MediaRecorder など。
     * @param width 横。映像に合わせる。
     * @param height 縦。映像に合わせる。
     */
    data class SurfaceRendering(
        val surface: Surface,
        override val width: Int,
        override val height: Int
    ) : AkariGraphicsProcessorRenderingPrepareData

    /**
     * オフスクリーンレンダリング用。
     * Surface が無いが、[io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor] を利用したい場合。
     *
     * @param width 横。映像に合わせる。
     * @param height 縦。映像に合わせる。
     */
    data class OffscreenRendering(
        override val width: Int,
        override val height: Int
    ) : AkariGraphicsProcessorRenderingPrepareData
}