package io.github.takusan23.akaridroid.canvasrender.itemrender

import io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture

/** [io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer.drawSurfaceTexture]する。SurfaceTexture のテクスチャを描画する */
interface DrawSurfaceTexture {

    val akariGraphicsSurfaceTexture: AkariGraphicsSurfaceTexture

    /** 描画する前に呼ばれる。描画位置を決定する行列[mvpMatrix]をここで設定する。 */
    fun draw(
        mvpMatrix: FloatArray,
        outputWidth: Int,
        outputHeight: Int
    )

}