package io.github.takusan23.akaricore.video

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import io.github.takusan23.akaricore.video.gl.InputSurface
import io.github.takusan23.akaricore.video.gl.ShaderImageRenderer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

/** OpenGL ES の GLSL、フラグメントシェーダーで画像を加工する */
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
        // TODO VideoFrameBitmapExtractor と同じハックを利用
        val maxSize = maxOf(width, height)
        val imageReaderSize = when {
            maxSize < 320 -> 320
            maxSize < 480 -> 480
            maxSize < 720 -> 720
            maxSize < 1280 -> 1280
            maxSize < 1920 -> 1920
            maxSize < 2560 -> 2560
            maxSize < 3840 -> 3840
            else -> 1920 // 何もなければ適当に Full HD
        }
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

}