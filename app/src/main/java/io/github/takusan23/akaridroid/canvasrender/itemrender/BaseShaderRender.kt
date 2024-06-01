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
 *
 * デフォルトの uniform 変数は、[GpuShaderImageProcessor]を参照してください。
 * また、[ShaderRender]では、以下の uniform 変数が動画編集用に用意されています。
 *
 * ## uniform float f_time;
 * 素材が開始した時間から、素材が終わるまでを 0~1 でセットします。
 * 動画の再生位置ではありません。
 * 必要ない場合は利用しなくても大丈夫です。
 *
 * abstract class なのは、以下はすべてフラグメントシェーダーでフレームを処理する。共通しているため。
 * 両方とも GLSL を使って、動画のフレームにエフェクトを適用している。
 *
 * - [ShaderRender]
 * - [SwitchAnimationRender]
 * - [EffectRender]
 */
abstract class BaseShaderRender : BaseItemRender() {

    /** Bitmap を GLSL で加工する */
    private var gpuShaderImageProcessor: GpuShaderImageProcessor? = null

    private val paint = Paint()

    abstract override val layerIndex: Int

    abstract val fragmentShader: String

    abstract val size: RenderData.Size

    abstract val position: RenderData.Position

    abstract val displayTime: RenderData.DisplayTime

    override suspend fun prepare() {
        try {
            val processor = GpuShaderImageProcessor().apply {
                prepare(
                    fragmentShaderCode = fragmentShader,
                    width = size.width,
                    height = size.height
                )
            }
            // 初期化に成功すれば
            gpuShaderImageProcessor = processor
            // f_time uniform 変数を追加する
            processor.addCustomFloatUniformHandle(UNIFORM_NAME_F_TIME)
        } catch (e: Exception) {
            // シェーダーのミス等
            e.printStackTrace()
        }
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
        gpuShaderImageProcessor?.destroy()
    }

    abstract override suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean

    override suspend fun isDisplayPosition(currentPositionMs: Long): Boolean {
        return currentPositionMs in displayTime
    }

    companion object {
        /** 素材の開始から終了までを 0~1 で表す uniform 変数名 */
        private const val UNIFORM_NAME_F_TIME = "f_time"
    }

}