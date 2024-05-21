package io.github.takusan23.akaricore.video

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import io.github.takusan23.akaricore.video.GpuShaderImageProcessor.Companion.FRAGMENT_SHADER_TEXTURE_RENDER
import io.github.takusan23.akaricore.video.gl.InputSurface
import io.github.takusan23.akaricore.video.gl.ShaderImageRenderer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

/**
 * OpenGL ES の GLSL、フラグメントシェーダーで画像を加工する。
 * OpenGL ES なので、名前の通り GPU 側で画像の各ピクセルを操作します。
 *
 * # シェーダーについて
 * フラグメントシェーダーのみを受け付けます。バーテックスシェーダーは画面いっぱいを描画するようなシェーダーを設定済みです。
 *
 * # フラグメントシェーダーについて
 * OpenGL ES バージョンは 2.0 です。in / out は 3.0 の機能なので注意です。
 * テクスチャ（画像）を表示するだけのサンプルはこちらです。[FRAGMENT_SHADER_TEXTURE_RENDER]
 * 以下の uniform 変数が利用できます。
 *
 * ## uniform sampler2D s_texture;
 * [drawShader]の引数[Bitmap]は、この s_texture でテクスチャとして利用できます。
 * texture2D() に入れて使ってください。
 *
 * ## uniform vec2 v_resolution;
 * これは画面の width / height を入れている vec2 です。vec2(width, height) です。
 * 座標の正規化に使ってください。
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GpuShaderImageProcessor {

    /** OpenGL 用に用意した描画用スレッド。Kotlin coroutines では Dispatcher を切り替えて使う */
    private val openGlRendererThreadDispatcher = newSingleThreadContext("openGlRendererThreadDispatcher")

    /** OpenGL ES で描画した画面を Bitmap として取り出すための ImageReader。glReadPixels でもいいけど、Android はこれがあるので。 */
    private var imageReader: ImageReader? = null

    /** ImageReader と OpenGL ES と繋ぐやつ */
    private var inputSurface: InputSurface? = null

    /** フラグメントシェーダーを受け取って、OpenGL ES で描画するやつ */
    private var shaderImageRenderer: ShaderImageRenderer? = null

    /**
     * 初期設定を行う
     *
     * @param width [ImageReader]の幅。[drawShader]で渡す[Bitmap]と同じでいいはず。
     * @param height [ImageReader]の高さ。[drawShader]で渡す[Bitmap]と同じでいいはず。
     */
    suspend fun prepare(
        fragmentShaderCode: String,
        width: Int,
        height: Int
    ) {
        // TODO VideoFrameBitmapExtractor と同じハックが必要か確認
        // val maxSize = maxOf(width, height)
        // val imageReaderSize = when {
        //     maxSize < 320 -> 320
        //     maxSize < 480 -> 480
        //     maxSize < 720 -> 720
        //     maxSize < 1280 -> 1280
        //     maxSize < 1920 -> 1920
        //     maxSize < 2560 -> 2560
        //     maxSize < 3840 -> 3840
        //     else -> 1920 // 何もなければ適当に Full HD
        // }
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        inputSurface = InputSurface(outputSurface = imageReader!!.surface)

        // 画像にエフェクトをかけるための TextureRenderer
        shaderImageRenderer = ShaderImageRenderer(
            fragmentShaderCode = fragmentShaderCode,
            width = width,
            height = height
        )

        // OpenGL の関数を呼ぶ際は、描画用スレッドに切り替えてから
        withContext(openGlRendererThreadDispatcher) {
            inputSurface?.makeCurrent()
            shaderImageRenderer?.createRenderer()
        }
    }

    /**
     * フラグメントシェーダーに[Bitmap]を渡して、描画する。
     *
     * @param bitmap [Bitmap]
     * @return 加工された[Bitmap]
     */
    suspend fun drawShader(
        bitmap: Bitmap
    ): Bitmap? {
        // 描画する
        // GL スレッドで
        withContext(openGlRendererThreadDispatcher) {
            shaderImageRenderer?.draw(bitmap)
            inputSurface?.swapBuffers()
        }
        // ImageReader で受け取る
        // Bitmap に
        return imageReader?.getImageReaderBitmap(
            fixWidth = bitmap.width,
            fixHeight = bitmap.height
        )
    }

    /** 破棄する */
    fun destroy() {
        openGlRendererThreadDispatcher.close()
        imageReader?.close()
        inputSurface?.destroy()
        shaderImageRenderer?.destroy()
    }

    companion object {

        /** 画像を表示するだけの、最低限のシェーダー */
        const val FRAGMENT_SHADER_TEXTURE_RENDER = """precision mediump float;

uniform sampler2D s_texture;
uniform vec2 v_resolution;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / v_resolution.xy;
    // 反転しているので
    uv = vec2(uv.x, 1.-uv.y);
    // 色を出す
    vec4 color = texture2D(s_texture, uv);
    gl_FragColor = color;
}
"""
    }

}