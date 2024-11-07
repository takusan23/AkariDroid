package io.github.takusan23.akaridroid.canvasrender.itemrender.feature

import io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture

/**
 * [io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor]で[AkariGraphicsSurfaceTexture]を描画する。
 * 動画のデコード結果。
 */
interface DrawSurfaceTextureInterface {

    /** [AkariGraphicsSurfaceTexture] */
    val akariGraphicsSurfaceTexture: AkariGraphicsSurfaceTexture

    /**
     * 描画する前に呼ばれる。
     * サイズや位置を決定する行列[mvpMatrix]をここで設定する。
     * [android.opengl.Matrix]を使って操作する。
     *
     * @param mvpMatrix 行列
     * @param width 幅
     * @param height 高さ
     */
    fun draw(
        mvpMatrix: FloatArray,
        width: Int,
        height: Int
    )

}