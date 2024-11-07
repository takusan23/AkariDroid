package io.github.takusan23.akaridroid.canvasrender.itemrender

import io.github.takusan23.akaricore.graphics.AkariGraphicsEffectShader
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.DrawFragmentShaderInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.GlTimelineLifecycleInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.feature.ProcessorDestroyInterface

// TODO コメント書き直す
// TODO "#version 300 es"が使えるようにする
abstract class BaseShaderRenderer : GlTimelineLifecycleInterface(), DrawFragmentShaderInterface, ProcessorDestroyInterface {

    /** [io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor]にエフェクトを適用するため */
    override var akariGraphicsEffectShader: AkariGraphicsEffectShader? = null

    abstract override val layerIndex: Int

    abstract val fragmentShader: String

    abstract val size: RenderData.Size

    abstract val position: RenderData.Position

    abstract val displayTime: RenderData.DisplayTime

    override suspend fun enterTimelineGl() {
        super.enterTimelineGl()
        try {
            akariGraphicsEffectShader = AkariGraphicsEffectShader(
                vertexShaderCode = AkariGraphicsEffectShader.VERTEX_SHADER_GLSL100,
                fragmentShaderCode = fragmentShader
            ).apply {
                // コンパイル
                prepareShader()
                // 必要な uniform 変数を探す
                findVec4UniformLocation(UNIFORM_NAME_CROP_LOCATION)
                findFloatUniformLocation(UNIFORM_NAME_F_TIME)
            }
        } catch (e: Exception) {
            // シェーダーのミス等
            e.printStackTrace(System.out)
        }
    }

    override suspend fun leaveTimelineGl() {
        super.leaveTimelineGl()
        akariGraphicsEffectShader?.destroy()
        akariGraphicsEffectShader = null
    }

    override suspend fun destroyProcessorGl() {
        isEnterTimeline = false
        akariGraphicsEffectShader?.destroy()
        akariGraphicsEffectShader = null
    }

    abstract override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in displayTime
    }

    override fun preEffect(width: Int, height: Int, durationMs: Long, currentPositionMs: Long) {
        // 描画するべき範囲を渡す
        val relativeX = position.x / width
        val relativeY = position.y / height
        akariGraphicsEffectShader?.setVec4Uniform(
            uniformName = UNIFORM_NAME_CROP_LOCATION,
            float1 = relativeX, // xStart
            float2 = relativeX + (size.width / width.toFloat()), // xEnd
            // OpenGL テクスチャ座標は反転しているので注意
            float3 = 1f - (relativeY + (size.height / height.toFloat())), // yEnd
            float4 = 1f - relativeY, // yStart
        )

        // 素材の開始から終了までを 0~1 で計算して uniform 変数に渡す
        val positionInRenderItem = currentPositionMs - displayTime.startMs
        val progressInRenderItem = positionInRenderItem / displayTime.durationMs.toFloat()
        akariGraphicsEffectShader?.setFloatUniform(UNIFORM_NAME_F_TIME, progressInRenderItem)
    }

    companion object {
        /** 素材の開始から終了までを 0~1 で表す uniform 変数名 */
        private const val UNIFORM_NAME_F_TIME = "f_time"

        /** 範囲を vec4 で渡す uniform 変数。vec4(xStart,xEnd,yStart,yEnd) */
        private const val UNIFORM_NAME_CROP_LOCATION = "vCropLocation"
    }
}