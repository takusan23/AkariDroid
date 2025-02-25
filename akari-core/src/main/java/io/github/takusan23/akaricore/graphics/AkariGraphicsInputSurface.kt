package io.github.takusan23.akaricore.graphics

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLExt
import javax.microedition.khronos.egl.EGL10

/**
 * MediaCodec で描画する際に OpenGL ES の設定が必要だが、EGL 周りの設定をしてくれるやつ。
 * EGL 1.4 、GLES 3.0 でセットアップする。GL スレッドから呼び出すこと。
 *
 * TODO 10-bit HDR （HLG 形式）の描画に対応しているかを確認する方法を用意する
 * TODO HLG だけじゃなく PQ にも対応する。多分定数入れ替えるだけ。
 *
 * @param renderingMode OpenGL ES の描画先
 * @param isEnableTenBitHdr 10-bit HDR を利用する場合は true
 */
internal class AkariGraphicsInputSurface(
    renderingMode: AkariGraphicsProcessorRenderingMode,
    private val isEnableTenBitHdr: Boolean
) {
    private var mEGLDisplay = EGL14.EGL_NO_DISPLAY
    private var mEGLContext = EGL14.EGL_NO_CONTEXT
    private var mEGLSurface = EGL14.EGL_NO_SURFACE

    init {
        // 10-bit HDR のためには HLG の表示が必要。
        // それには OpenGL ES 3.0 でセットアップし、10Bit に設定する必要がある。
        if (isEnableTenBitHdr) {
            eglSetupForTenBitHdr(renderingMode)
        } else {
            eglSetupForSdr(renderingMode)
        }
    }

    /** 10-bit HDR version. Prepares EGL. We want a GLES 3.0 context and a surface that supports recording. */
    private fun eglSetupForTenBitHdr(renderingMode: AkariGraphicsProcessorRenderingMode) {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw RuntimeException("unable to initialize EGL14")
        }
        // Configure EGL for recording and OpenGL ES 3.0.
        val attribList = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_RED_SIZE, 10,
            EGL14.EGL_GREEN_SIZE, 10,
            EGL14.EGL_BLUE_SIZE, 10,
            EGL14.EGL_ALPHA_SIZE, 2,
            EGL14.EGL_SURFACE_TYPE, (EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT),
            // EGL_RECORDABLE_ANDROID, 1, // RGBA1010102 だと使えないし多分いらない
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
        checkEglError("eglCreateContext RGBA1010102 ES3")

        // Configure context for OpenGL ES 3.0.
        val attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL14.EGL_NONE
        )
        mEGLContext = EGL14.eglCreateContext(
            mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            attrib_list, 0
        )
        checkEglError("eglCreateContext")

        // EGL_GL_COLORSPACE_BT2020_HLG_EXT を使うことで OpenGL ES で HDR 表示が可能になる（HLG 形式）
        // TODO 10-bit HDR（BT2020 / HLG）に対応していない端末で有効にした場合にエラーになる。とりあえず対応していない場合は SDR にフォールバックする
        // TODO HLG だけじゃなく PQ も対応する
        val appendSurfaceAttribs = if (isAvailableExtension("EGL_EXT_gl_colorspace_bt2020_hlg")) {
            intArrayOf(EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_HLG_EXT)
        } else {
            intArrayOf()
        }
        // 描画先
        // Create a window surface, and attach it to the Surface we received.
        mEGLSurface = when (renderingMode) {
            is AkariGraphicsProcessorRenderingMode.OffscreenRendering -> {
                // オフスクリーンレンダリングの場合は横と縦が必要？
                // https://github.com/google-ai-edge/mediapipe/blob/ffe429d5278b914c44fdb5df3ce38962b55580bb/mediapipe/java/com/google/mediapipe/glutil/EglManager.java#L203
                val surfaceAttribs = appendSurfaceAttribs + intArrayOf(
                    EGL10.EGL_WIDTH, renderingMode.width,
                    EGL10.EGL_HEIGHT, renderingMode.height,
                    EGL14.EGL_NONE
                )
                EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0)
            }

            is AkariGraphicsProcessorRenderingMode.SurfaceRendering -> {
                val surfaceAttribs = appendSurfaceAttribs + intArrayOf(
                    EGL14.EGL_NONE
                )
                EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], renderingMode.surface, surfaceAttribs, 0)
            }
        }
        checkEglError("eglCreateWindowSurface")
        checkEglError("eglCreateWindowSurface")
    }

    /** Prepares EGL. We want a GLES 3.0 context and a surface that supports recording. */
    private fun eglSetupForSdr(renderingMode: AkariGraphicsProcessorRenderingMode) {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw RuntimeException("unable to initialize EGL14")
        }
        // Configure EGL for recording and OpenGL ES 3.0.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
        checkEglError("eglCreateContext RGB888 ES3")

        // Configure context for OpenGL ES 3.0.
        val attrib_list = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL14.EGL_NONE
        )
        mEGLContext = EGL14.eglCreateContext(
            mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
            attrib_list, 0
        )
        checkEglError("eglCreateContext")

        // Create a window surface, and attach it to the Surface we received.
        mEGLSurface = when (renderingMode) {
            is AkariGraphicsProcessorRenderingMode.OffscreenRendering -> {
                // オフスクリーンレンダリングの場合は横と縦が必要？
                // https://github.com/google-ai-edge/mediapipe/blob/ffe429d5278b914c44fdb5df3ce38962b55580bb/mediapipe/java/com/google/mediapipe/glutil/EglManager.java#L203
                val surfaceAttribs = intArrayOf(
                    EGL10.EGL_WIDTH, renderingMode.width,
                    EGL10.EGL_HEIGHT, renderingMode.height,
                    EGL14.EGL_NONE
                )
                EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0)
            }

            is AkariGraphicsProcessorRenderingMode.SurfaceRendering -> {
                val surfaceAttribs = intArrayOf(
                    EGL14.EGL_NONE
                )
                EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], renderingMode.surface, surfaceAttribs, 0)
            }
        }
        checkEglError("eglCreateWindowSurface")
    }

    /** Discards all resources held by this class, notably the EGL context. */
    fun destroy() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(mEGLDisplay)
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY
        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEGLSurface = EGL14.EGL_NO_SURFACE
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)
        checkEglError("eglMakeCurrent")
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    fun swapBuffers(): Boolean {
        val result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface)
        checkEglError("eglSwapBuffers")
        return result
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    fun setPresentationTime(nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs)
        checkEglError("eglPresentationTimeANDROID")
    }

    /**
     * Checks for EGL errors.  Throws an exception if one is found.
     */
    private fun checkEglError(msg: String) {
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            throw RuntimeException("$msg: EGL error: 0x${Integer.toHexString(error)}")
        }
    }

    /**
     * OpenGL ES の拡張機能をサポートしているか。
     * 例えば 10-bit HDR を描画する機能は新し目の Android にしか無いため
     *
     * @param extensionName "EGL_EXT_gl_colorspace_bt2020_hlg" など
     * @return 拡張機能をサポートしている場合は true
     */
    private fun isAvailableExtension(extensionName: String): Boolean {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val eglExtensions = EGL14.eglQueryString(display, EGL10.EGL_EXTENSIONS)
        return eglExtensions != null && eglExtensions.contains(extensionName)
    }

    companion object {
        private const val EGL_RECORDABLE_ANDROID = 0x3142

        // HDR 表示に必要
        private const val EGL_GL_COLORSPACE_KHR = 0x309D
        private const val EGL_GL_COLORSPACE_BT2020_HLG_EXT = 0x3540

    }

}