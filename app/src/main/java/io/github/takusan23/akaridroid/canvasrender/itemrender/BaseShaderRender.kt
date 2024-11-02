package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import io.github.takusan23.akaricore.graphics.AkariGraphicsEffectShader
import io.github.takusan23.akaricore.video.GpuShaderImageProcessor
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO コメント書き直す
abstract class BaseShaderRender : BaseItemRender(), DrawFragmentShader {

    /** Bitmap を GLSL で加工する */
    private var gpuShaderImageProcessor: GpuShaderImageProcessor? = null

    private val paint = Paint()

    abstract override val layerIndex: Int

    abstract val fragmentShader: String

    abstract val size: RenderData.Size

    abstract val position: RenderData.Position

    abstract val displayTime: RenderData.DisplayTime

    override var akariGraphicsEffectShader: AkariGraphicsEffectShader? = null

    // TODO GLスレッドから呼び出す。prepare() - destroy() で生成 - 破棄するため
    override suspend fun prepare() {
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

    override fun setVideoSize(width: Int, height: Int) {
        val relativeX = position.x / width
        val relativeY = position.y / height

        // 描画するべき範囲を渡す
        akariGraphicsEffectShader?.setVec4Uniform(
            uniformName = UNIFORM_NAME_CROP_LOCATION,
            float1 = relativeX, // xStart
            float2 = relativeX + (size.width / width.toFloat()), // xEnd
            // OpenGL テクスチャ座標は反転しているので注意
            float3 = 1f - (relativeY + (size.height / height.toFloat())), // yEnd
            float4 = 1f - relativeY, // yStart
        )
    }

    override fun preEffect(durationMs: Long, currentPositionMs: Long) {
        // 素材の開始から終了までを 0~1 で計算して uniform 変数に渡す
        val positionInRenderItem = currentPositionMs - displayTime.startMs
        val progressInRenderItem = positionInRenderItem / displayTime.durationMs.toFloat()
        akariGraphicsEffectShader?.setFloatUniform(UNIFORM_NAME_F_TIME, progressInRenderItem)
    }

    override suspend fun draw(canvas: Canvas, drawFrame: Bitmap, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.Default) {
        val x = position.x.toInt()
        val y = position.y.toInt()
        val (width, height) = size

        // 素材の開始から終了までを 0~1 で計算する
        val positionInRenderItem = currentPositionMs - displayTime.startMs
        val progressInRenderItem = positionInRenderItem / displayTime.durationMs.toFloat()
        gpuShaderImageProcessor?.setCustomFloatUniform(UNIFORM_NAME_F_TIME, progressInRenderItem)

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
        akariGraphicsEffectShader?.destroy()
        akariGraphicsEffectShader = null
    }

    abstract override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in displayTime
    }

    companion object {
        /** 素材の開始から終了までを 0~1 で表す uniform 変数名 */
        private const val UNIFORM_NAME_F_TIME = "f_time"

        /** 範囲を vec4 で渡す uniform 変数。vec4(xStart,xEnd,yStart,yEnd) */
        private const val UNIFORM_NAME_CROP_LOCATION = "vCropLocation"
    }

}