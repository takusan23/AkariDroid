package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import io.github.takusan23.akaricore.video.GpuShaderImageProcessor
import io.github.takusan23.akaridroid.RenderData

/**
 * フラグメントシェーダーで[Bitmap]を描画する。
 * 各フレームを[Bitmap]で受け取って、GLSL のフラグメントシェーダーでエフェクトを適用するのに使えます。
 * uniform 変数等は、[GpuShaderImageProcessor]を参照してください。
 */
class ShaderRender(
    private val shader: RenderData.CanvasItem.Shader
) : BaseItemRender() {

    /** Bitmap を GLSL で加工する */
    private var gpuShaderImageProcessor: GpuShaderImageProcessor? = null

    private val paint = Paint()

    override val layerIndex: Int
        get() = shader.layerIndex

    override suspend fun prepare() {
        try {
            val processor = GpuShaderImageProcessor().apply {
                prepare(
                    fragmentShaderCode = shader.fragmentShader,
                    width = shader.size.width,
                    height = shader.size.height
                )
            }
            // 初期化に成功すれば
            gpuShaderImageProcessor = processor
        } catch (e: Exception) {
            // シェーダーのミス等
            // TODO シェーダーのコンパイルが通るかの確認したい
            e.printStackTrace()
        }
    }

    override suspend fun draw(canvas: Canvas, drawFrame: Bitmap, durationMs: Long, currentPositionMs: Long) {
        // ここまでの描いたフレームを加工する
        val effectBitmap = gpuShaderImageProcessor?.drawShader(drawFrame) ?: return
        // Canvas に描く
        val (x, y) = shader.position
        canvas.drawBitmap(effectBitmap, x, y, paint)
    }

    override fun destroy() {
        gpuShaderImageProcessor?.destroy()
    }

    override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean {
        return shader == renderItem
    }

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in shader.displayTime
    }

}