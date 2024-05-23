package io.github.takusan23.akaridroid.canvasrender.itemrender

import io.github.takusan23.akaridroid.RenderData

/**
 * ユーザーが入力したフラグメントシェーダーで、各動画フレームにエフェクトを適用する
 * 詳しくは[BaseShaderRender]で。
 */
class ShaderRender(
    private val shader: RenderData.CanvasItem.Shader
) : BaseShaderRender() {
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