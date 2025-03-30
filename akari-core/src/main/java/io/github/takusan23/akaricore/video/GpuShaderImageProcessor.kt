package io.github.takusan23.akaricore.video

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PixelFormat
import android.media.ImageReader
import io.github.takusan23.akaricore.graphics.AkariGraphicsEffectShader
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorColorSpaceType
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorRenderingPrepareData

/**
 * OpenGL ES の GLSL、フラグメントシェーダーで画像を加工する。
 * OpenGL ES なので、名前の通り GPU 側で画像の各ピクセルを操作します。
 *
 * # シェーダーについて
 * フラグメントシェーダーのみを受け付けます。バーテックスシェーダーは画面いっぱいを描画するようなシェーダーを設定済みです。
 *
 * # フラグメントシェーダーについて
 * OpenGL ES バージョンは 2.0 です。in / out は 3.0 の機能なので注意です。
 * 3.0 を使いたい場合は[AkariGraphicsProcessor]をそのまま使うことをおすすめします。こっちは残してあるだけなので。
 *
 * # Uniform 変数について
 * [AkariGraphicsEffectShader]と同じです。
 */
class GpuShaderImageProcessor {

    /** Canvas で Bitmap 書くときの */
    private val paint = Paint()

    /** OpenGL ES で描画した画面を Bitmap として取り出すための ImageReader。glReadPixels でもいいけど、Android はこれがあるので。 */
    private var imageReader: ImageReader? = null

    /** OpenGL ES で描画するやつ */
    private var akariGraphicsProcessor: AkariGraphicsProcessor? = null

    /** フラグメントシェーダーでエフェクトをかける */
    private var akariGraphicsEffectShader: AkariGraphicsEffectShader? = null

    // ImageReader は、1280x720 とかのきれいな数字の場合は動くが、半端な数字を入れた途端、出力された映像がぐちゃぐちゃになる。
    // それを回避するため、半端な数字が来た場合は、一番近い数字に丸める。
    // ただ、近い数字に丸めてしまうと画像サイズが変わってしまうため、元々のサイズに戻せるようここに持っておく。
    private var originWidth: Int? = null // 元のはば
    private var originHeight: Int? = null // 元のたかさ

    /**
     * 初期設定を行う
     *
     * @param fragmentShaderCode フラグメントシェーダー。コンパイルに失敗すると例外が投げられます
     * @param width [ImageReader]の幅。[drawShader]で渡す[Bitmap]と同じでいいはず。
     * @param height [ImageReader]の高さ。[drawShader]で渡す[Bitmap]と同じでいいはず。
     */
    suspend fun prepare(
        fragmentShaderCode: String = FRAGMENT_SHADER_TEXTURE_RENDER,
        width: Int,
        height: Int
    ) {
        // VideoFrameBitmapExtractor と同じハックが必要。
        // VideoFrameBitmapExtractor と違って nearestImageReaderAvailableSize が必要。toFixImageReaderSupportValue だけだと乱れてしまった。
        // TODO 多分 GpuShaderImageProcessor の方は nearestImageReaderAvailableSize が必要。
        originWidth = width
        originHeight = height
        val fixWidth = originWidth!!.toFixImageReaderSupportValue()
        val fixHeight = originHeight!!.toFixImageReaderSupportValue()
        val nearestSize = nearestImageReaderAvailableSize(fixWidth, fixHeight)

        // 縦横同じだが、後で戻す
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        akariGraphicsProcessor = AkariGraphicsProcessor(
            renderingPrepareData = AkariGraphicsProcessorRenderingPrepareData.SurfaceRendering(
                surface = imageReader!!.surface,
                width = width,
                height = height
            ),
            colorSpaceType = AkariGraphicsProcessorColorSpaceType.SDR_BT709
        ).apply { prepare() }

        akariGraphicsEffectShader = AkariGraphicsEffectShader(
            vertexShaderCode = AkariGraphicsEffectShader.VERTEX_SHADER_GLSL100,
            fragmentShaderCode = fragmentShaderCode
        )
        akariGraphicsProcessor!!.withOpenGlThread {
            akariGraphicsEffectShader!!.prepareShader()
        }
    }

    /** Uniform 変数を登録する */
    suspend fun addCustomFloatUniformHandle(uniformName: String) {
        val akariGraphicsProcessor = akariGraphicsProcessor!!
        val akariGraphicsEffectShader = akariGraphicsEffectShader!!

        akariGraphicsProcessor.withOpenGlThread {
            akariGraphicsEffectShader.findFloatUniformLocation(uniformName)
        }
    }

    /** Uniform 変数を更新する */
    suspend fun setCustomFloatUniform(uniformName: String, value: Float) {
        val akariGraphicsProcessor = akariGraphicsProcessor!!
        val akariGraphicsEffectShader = akariGraphicsEffectShader!!

        akariGraphicsProcessor.withOpenGlThread {
            akariGraphicsEffectShader.setFloatUniform(uniformName, value)
        }
    }

    /**
     * フラグメントシェーダーに[Bitmap]を渡して、描画する。
     *
     * @param bitmap [Bitmap]
     * @return 加工された[Bitmap]
     */
    suspend fun drawShader(bitmap: Bitmap): Bitmap? {
        val akariGraphicsProcessor = akariGraphicsProcessor!!
        val akariGraphicsEffectShader = akariGraphicsEffectShader!!
        val originWidth = originWidth!!
        val originHeight = originHeight!!

        // 描画する
        // GL スレッドで
        akariGraphicsProcessor.drawOneshot {
            drawCanvas {
                drawBitmap(bitmap, 0f, 0f, paint)
            }
            applyEffect(akariGraphicsEffectShader)
        }

        // ImageReader で受け取る
        // Bitmap に
        return imageReader?.getImageReaderBitmap(
            fixWidth = originWidth,
            fixHeight = originHeight
        )
    }

    /** 破棄する */
    suspend fun destroy() {
        akariGraphicsProcessor?.destroy()
        akariGraphicsEffectShader?.destroy()
        imageReader?.close()
    }

    companion object {

        /** 画像を表示するだけの、最低限のシェーダー */
        const val FRAGMENT_SHADER_TEXTURE_RENDER = """precision mediump float;

uniform sampler2D sVideoFrameTexture;
uniform vec2 vResolution;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / vResolution.xy;
    // 反転しているので
    uv = vec2(uv.x, 1.-uv.y);
    // 色を出す
    vec4 color = texture2D(sVideoFrameTexture, uv);
    gl_FragColor = color;
}
"""
    }

}