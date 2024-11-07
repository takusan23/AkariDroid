package io.github.takusan23.akaridroid.canvasrender.itemrender.feature

import io.github.takusan23.akaricore.graphics.AkariGraphicsEffectShader

/** [io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor]でエフェクトを適用するもの */
interface DrawFragmentShaderInterface {

    /** [AkariGraphicsEffectShader] */
    var akariGraphicsEffectShader: AkariGraphicsEffectShader?

    /**
     * エフェクトを適用する前に呼び出される。
     * uniform 変数へのセットなど。
     *
     * @param width 幅
     * @param height 高さ
     * @param durationMs 動画の時間
     * @param currentPositionMs 動画の再生位置
     */
    fun preEffect(width: Int, height: Int, durationMs: Long, currentPositionMs: Long)

}