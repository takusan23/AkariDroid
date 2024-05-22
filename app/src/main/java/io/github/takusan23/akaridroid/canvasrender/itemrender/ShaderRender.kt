package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import io.github.takusan23.akaricore.video.GpuShaderImageProcessor
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    override suspend fun draw(canvas: Canvas, drawFrame: Bitmap, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.Default) {
        val x = shader.position.x.toInt()
        val y = shader.position.y.toInt()
        val (width, height) = shader.size

        // Android Canvas にある Rect を使って、重なる部分（くり抜く部分）を求める
        // 自前で計算するのはめんどい。。。
        val resultRect = Rect()
        val drawFrameBitmapRect = Rect(0, 0, drawFrame.width, drawFrame.height)
        val cropEffectRect = Rect(x, y, width + x, height + y)
        // 交差する（重なる部分を出す）
        val isSuccess = resultRect.setIntersect(drawFrameBitmapRect, cropEffectRect)
        // ない場合は戻る
        if (!isSuccess) return@withContext

        // 動画のフレームから、シェーダーを適用する部分だけくり抜く
        // 動画のフレームへ部分的にシェーダーを適用できるようにする
        // サイズオーバー用の考慮が入っている
        val cropBitmap = Bitmap.createBitmap(
            drawFrame,
            resultRect.left,
            resultRect.top,
            resultRect.width(),
            resultRect.height()
        )
        // ここまでの描いたフレームを加工する
        val effectBitmap = gpuShaderImageProcessor?.drawShader(cropBitmap) ?: return@withContext
        // Canvas に描く
        canvas.drawBitmap(effectBitmap, resultRect.left.toFloat(), resultRect.top.toFloat(), paint)
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