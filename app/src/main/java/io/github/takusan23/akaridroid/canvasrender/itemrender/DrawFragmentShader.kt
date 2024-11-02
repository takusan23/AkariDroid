package io.github.takusan23.akaridroid.canvasrender.itemrender

import io.github.takusan23.akaricore.graphics.AkariGraphicsEffectShader

interface DrawFragmentShader {

    var akariGraphicsEffectShader: AkariGraphicsEffectShader?

    fun setVideoSize(width: Int, height: Int)

    fun preEffect(durationMs: Long, currentPositionMs: Long)
}