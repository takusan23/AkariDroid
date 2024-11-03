package io.github.takusan23.akaridroid.canvasrender.itemrender.v2

import io.github.takusan23.akaridroid.RenderData

/** ユーザーが入力したフラグメントシェーダーで、動画フレームにエフェクトを適用する */
class ShaderRenderer(private val shader: RenderData.CanvasItem.Shader) : BaseShaderRenderer() {

    override val layerIndex: Int
        get() = shader.layerIndex

    override val fragmentShader: String
        get() = shader.fragmentShader

    override val size: RenderData.Size
        get() = shader.size

    override val position: RenderData.Position
        get() = shader.position

    override val displayTime: RenderData.DisplayTime
        get() = shader.displayTime

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return shader == renderItem
    }

}