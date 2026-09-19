package io.github.takusan23.akaricore.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.milliseconds

/**
 * [AkariGraphicsProcessor]で描画を担当。フラグメントシェーダーとかを使ってる。
 * インスタンス化はライブラリ側でやるため internal constructor になります。
 * クラス自体も public ですが、[drawCanvas]や[drawSurfaceTexture]のためなので、ライブラリ利用側から使われたくない関数はすべて internal fun にしています。
 */
class AkariGraphicsTextureRenderer internal constructor(
    private val width: Int,
    private val height: Int,
    private val isEnableTenBitHdr: Boolean
) {
    private val mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.size * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val mMVPMatrix = FloatArray(16)
    private val mSTMatrix = FloatArray(16)

    // OpenGL ES のプログラムを描画別で作った
    // 一つのフラグメントシェーダーを使い描画内容を分岐する方法を使っていたが、一部の GPU（Pixel 11 ANGLE）では __samplerExternal2DY2YEXT を使う使わない関係なくシェーダーで定義した以上渡さないとエラーになってしまった
    // glDrawArrays: glError 1282
    // Canvas の内容を描画するときは __samplerExternal2DY2YEXT は渡されない状態なので動かなかった。
    // ので、それぞれでフラグメントシェーダーを分けることにした
    private var hdrGlProgram: OpenGlProgram? = null
    private var sdrGlProgram: OpenGlProgram? = null
    private var canvasGlProgram: OpenGlProgram? = null
    private var fboGlProgram: OpenGlProgram? = null

    // テクスチャ ID
    private var surfaceTextureTextureId = 0
    private var canvasTextureTextureId = 0

    // Canvas 描画のため Bitmap
    private val canvasBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(canvasBitmap)

    // フレームバッファーオブジェクトを交互に使うやつ（ピンポンする）
    private var fboPingPongManager: FboPingPongManager? = null

    init {
        mTriangleVertices.put(mTriangleVerticesData).position(0)
    }

    /**
     * Canvas に書く。
     * GL スレッドから呼び出すこと。
     */
    suspend fun drawCanvas(draw: suspend Canvas.() -> Unit) {
        // 前回のを消す
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        // 書く
        draw(canvas)

        canvasGlProgram?.use()
        canvasGlProgram?.setActiveTexture(
            name = "sCanvasTexture",
            texId = canvasTextureTextureId,
            target = GLES20.GL_TEXTURE_2D,
            texUnit = GLES20.GL_TEXTURE1,
            texUnitIndex = 1
        )

        // テクスチャを転送
        // texImage2D、引数違いがいるので注意
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, canvasBitmap, 0)
        checkGlError("GLUtils.texImage2D")

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        canvasGlProgram?.setVertexAttribute(
            name = "aPosition",
            size = 3,
            type = GLES20.GL_FLOAT,
            normalized = false,
            stride = TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            pointer = mTriangleVertices
        )
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        canvasGlProgram?.setVertexAttribute(
            name = "aTextureCoord",
            size = 2,
            type = GLES20.GL_FLOAT,
            normalized = false,
            stride = TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            pointer = mTriangleVertices
        )

        // 行列をリセット
        Matrix.setIdentityM(mSTMatrix, 0)
        Matrix.setIdentityM(mMVPMatrix, 0)

        canvasGlProgram?.setMat4Uniform("uMVPMatrix", mMVPMatrix)
        canvasGlProgram?.setMat4Uniform("uSTMatrix", mSTMatrix)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        GLES20.glFinish()
    }

    @Deprecated("nullOrTextureUpdateTimeout の方を使ってください")
    suspend fun drawSurfaceTexture(
        akariSurfaceTexture: AkariGraphicsSurfaceTexture,
        isAwaitTextureUpdate: Boolean = false,
        onTransform: ((mvpMatrix: FloatArray) -> Unit)? = null,
        chromakeyThreshold: Float? = null,
        chromaKeyColor: Int? = null
    ) = drawSurfaceTexture(
        akariSurfaceTexture = akariSurfaceTexture,
        nullOrTextureUpdateTimeoutMs = if (isAwaitTextureUpdate) SURFACE_TEXTURE_UPDATE_TIMEOUT_MS else null,
        onTransform = onTransform,
        chromakeyThreshold = chromakeyThreshold,
        chromaKeyColor = chromaKeyColor
    )

    /**
     * SurfaceTexture を描画する。
     * GL スレッドから呼び出すこと。
     *
     * @param akariSurfaceTexture 描画する[AkariGraphicsSurfaceTexture]
     * @param nullOrTextureUpdateTimeoutMs カメラ映像や動画のデコードなど、テクスチャを更新する必要がある場合は、タイムアウトするまでの時間（ミリ秒）を入れるとそれまで更新を待ちます。更新する必要がない場合は null
     * @param onTransform 位置や回転を適用するための行列を作るための関数
     * @param chromakeyThreshold クロマキーする場合。クロマキーのしきい値
     * @param chromaKeyColor クロマキーする場合。クロマキーにする色
     */
    suspend fun drawSurfaceTexture(
        akariSurfaceTexture: AkariGraphicsSurfaceTexture,
        nullOrTextureUpdateTimeoutMs: Long? = null,
        onTransform: ((mvpMatrix: FloatArray) -> Unit)? = null,
        chromakeyThreshold: Float? = null,
        chromaKeyColor: Int? = null
    ) {
        // 一度も来ていない場合は、映像が到着するまで待つ
        akariSurfaceTexture.awaitAlreadyFrameAvailableCallback()
        val isHdr = akariSurfaceTexture.isHdr()

        // OpenGlProgram を選ぶ
        val videoFrameGlProgram = if (isHdr) hdrGlProgram else sdrGlProgram
        videoFrameGlProgram?.use()
        videoFrameGlProgram?.setActiveTexture(
            name = "sSurfaceTexture",
            texId = surfaceTextureTextureId,
            target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            texUnit = GLES20.GL_TEXTURE0,
            texUnitIndex = 0
        )

        // SurfaceTexture は後からテクスチャ ID を変えられる、適当な GL コンテキストで作ってもよい
        akariSurfaceTexture.detachGl()
        akariSurfaceTexture.attachGl(surfaceTextureTextureId)

        // タイムアウトが設定されている場合は、その時間だけ awaitUpdateTexImage() を試す
        if (nullOrTextureUpdateTimeoutMs != null) {
            withTimeoutOrNull(nullOrTextureUpdateTimeoutMs.milliseconds) {
                akariSurfaceTexture.awaitUpdateTexImage()
            }
        } else {
            akariSurfaceTexture.checkAndUpdateTexImage()
        }
        akariSurfaceTexture.getTransformMatrix(mSTMatrix)

        // クロマキーする場合
        // null の場合は 0 にして動かないように
        val chromakeyColor = chromaKeyColor?.toColorVec4() ?: floatArrayOf(0f, 0f, 0f, 0f)
        videoFrameGlProgram?.setFloatUniform(
            name = "chromakeyThreshold",
            value = chromakeyThreshold ?: 0f
        )
        videoFrameGlProgram?.setVec4Uniform(
            name = "chromakeyColor",
            float1 = chromakeyColor[0],
            float2 = chromakeyColor[1],
            float3 = chromakeyColor[2],
            float4 = chromakeyColor[3]
        )

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        videoFrameGlProgram?.setVertexAttribute(
            name = "aPosition",
            size = 3,
            type = GLES20.GL_FLOAT,
            normalized = false,
            stride = TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            pointer = mTriangleVertices
        )
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        videoFrameGlProgram?.setVertexAttribute(
            name = "aTextureCoord",
            size = 2,
            type = GLES20.GL_FLOAT,
            normalized = false,
            stride = TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            pointer = mTriangleVertices
        )

        // 行列を適用したい場合
        Matrix.setIdentityM(mMVPMatrix, 0)
        if (onTransform != null) {
            onTransform(mMVPMatrix)
        }

        videoFrameGlProgram?.setMat4Uniform("uMVPMatrix", mMVPMatrix)
        videoFrameGlProgram?.setMat4Uniform("uSTMatrix", mSTMatrix)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        GLES20.glFinish()
    }

    /**
     * [AkariGraphicsEffectShader]のエフェクトを適用する。
     * GL スレッドから呼び出すこと。
     */
    fun applyEffect(effectShader: AkariGraphicsEffectShader) {
        pingPongFrameBufferObject()

        // FBO のテクスチャユニットを渡して描画
        effectShader.applyEffect(width, height, 2) // GLES20.GL_TEXTURE2
    }

    /**
     * バーテックスシェーダ、フラグメントシェーダーをコンパイルする。
     * GL スレッドから呼び出すこと。
     */
    internal fun prepareShader() {

        // HDR 有効時のみコンパイル、通らんかもなので
        if (isEnableTenBitHdr) {
            hdrGlProgram = OpenGlProgram(
                vertexShaderCode = VERTEX_SHADER,
                fragmentShaderCode = FRAGMENT_SHADER_10BIT_HDR_YUV
            ).also { program ->
                // コンパイルと location を探しておく
                program.prepare()
                program.registerAttributeLocation("aPosition")
                program.registerAttributeLocation("aTextureCoord")
                program.registerMat4UniformLocation("uMVPMatrix")
                program.registerMat4UniformLocation("uSTMatrix")
                program.registerTextureUniformLocation("sSurfaceTexture")
                program.registerFloatUniformLocation("chromakeyThreshold")
                program.registerVec4UniformLocation("chromakeyColor")
            }
            checkGlError("glCreateProgram hdrGlProgram")
        }

        sdrGlProgram = OpenGlProgram(
            vertexShaderCode = VERTEX_SHADER,
            fragmentShaderCode = FRAGMENT_SHADER_SDR
        ).also { program ->
            program.prepare()
            program.registerAttributeLocation("aPosition")
            program.registerAttributeLocation("aTextureCoord")
            program.registerMat4UniformLocation("uMVPMatrix")
            program.registerMat4UniformLocation("uSTMatrix")
            program.registerTextureUniformLocation("sSurfaceTexture")
            program.registerFloatUniformLocation("chromakeyThreshold")
            program.registerVec4UniformLocation("chromakeyColor")
        }
        checkGlError("glCreateProgram sdrGlProgram")

        canvasGlProgram = OpenGlProgram(
            vertexShaderCode = VERTEX_SHADER,
            fragmentShaderCode = FRAGMENT_SHADER_CANVAS_BITMAP
        ).also { program ->
            program.prepare()
            program.registerAttributeLocation("aPosition")
            program.registerAttributeLocation("aTextureCoord")
            program.registerMat4UniformLocation("uMVPMatrix")
            program.registerMat4UniformLocation("uSTMatrix")
            program.registerTextureUniformLocation("sCanvasTexture")
        }
        checkGlError("glCreateProgram canvasGlProgram")

        fboGlProgram = OpenGlProgram(
            vertexShaderCode = VERTEX_SHADER,
            fragmentShaderCode = FRAGMENT_SHADER_FBO
        ).also { program ->
            program.prepare()
            program.registerAttributeLocation("aPosition")
            program.registerAttributeLocation("aTextureCoord")
            program.registerMat4UniformLocation("uMVPMatrix")
            program.registerMat4UniformLocation("uSTMatrix")
            program.registerTextureUniformLocation("sFboTexture")
        }
        checkGlError("glCreateProgram fboGlProgram")

        // テクスチャ ID を払い出してもらう
        // SurfaceTexture / Canvas Bitmap 用
        val textures = IntArray(2)
        GLES20.glGenTextures(2, textures, 0)

        // テクスチャユニットを登録、HDR はしてないがここは GL コンテキストに登録できていれば Program は関係ないはずなので、常にある SDR の方にした
        surfaceTextureTextureId = textures[0]
        sdrGlProgram?.setActiveTexture(
            name = "sSurfaceTexture",
            texId = surfaceTextureTextureId,
            target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            texUnit = GLES20.GL_TEXTURE0,
            texUnitIndex = 0
        )
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        checkGlError("glTexParameterf glTexParameteri")

        canvasTextureTextureId = textures[1]
        canvasGlProgram?.setActiveTexture(
            name = "sCanvasTexture",
            texId = canvasTextureTextureId,
            target = GLES20.GL_TEXTURE_2D,
            texUnit = GLES20.GL_TEXTURE1,
            texUnitIndex = 1
        )
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        checkGlError("glTexParameterf glTexParameteri")

        // アルファブレンディング
        // Canvas で書いた際に、透明な部分は透明になるように
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        checkGlError("glEnable GLES20.GL_BLEND")

        // フレームバッファオブジェクトの用意
        // ピンポンするため2つ（交互に利用。読み取り、書き込みを交互にする）
        val fbo1 = generateFrameBufferObject()
        val fbo2 = generateFrameBufferObject()

        // FBO テクスチャ ID を交互にするクラス
        fboPingPongManager = FboPingPongManager(fbo1, fbo2)
    }

    /**
     * 描画前に呼び出す。描画先を FBO にします。
     * GL スレッドから呼び出すこと。
     */
    internal fun prepareDraw() {
        pingPongFrameBufferObject()

        // FBO のクリア？多分必要
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
    }

    /**
     * テクスチャ ID を払い出す。
     * [AkariGraphicsSurfaceTexture]はコンストラクタでテクスチャ ID を必要とするが、描画時には[surfaceTextureTextureId]に切り替える。作成のためだけに必要。
     * 破棄する場合は使う側で呼び出してください。
     *
     * @param T 返り値
     * @param action 関数
     */
    internal fun <T> genTextureId(action: (texId: Int) -> T): T {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        return action(textures.first())
    }

    /** [AkariGraphicsEffectShader]を作る。詳しくは[AkariGraphicsProcessor]で。 */
    internal suspend fun <T> genEffect(action: suspend () -> T): T {
        return action()
    }

    /**
     * 最後に呼び出す。
     * フレームバッファオブジェクトのテクスチャを描画します。これでオフスクリーンで描画されてた内容が画面に表示されるはず。
     */
    internal fun drawEnd() {
        // TEXTURE2 へ前回の中身を移す
        pingPongFrameBufferObject()

        // フラグメントシェーダーを切り替える
        fboGlProgram?.use()

        // pingPongFrameBufferObject() したけど、最後なので描画先をデフォルトの FBO にして、Surface に描画されるように
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        checkGlError("glBindFramebuffer")

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        fboGlProgram?.setVertexAttribute(
            name = "aPosition",
            size = 3,
            type = GLES20.GL_FLOAT,
            normalized = false,
            stride = TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            pointer = mTriangleVertices
        )
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        fboGlProgram?.setVertexAttribute(
            name = "aTextureCoord",
            size = 2,
            type = GLES20.GL_FLOAT,
            normalized = false,
            stride = TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
            pointer = mTriangleVertices
        )

        // 行列をリセット
        Matrix.setIdentityM(mSTMatrix, 0)
        Matrix.setIdentityM(mMVPMatrix, 0)

        fboGlProgram?.setMat4Uniform("uSTMatrix", mSTMatrix)
        fboGlProgram?.setMat4Uniform("uMVPMatrix", mMVPMatrix)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        GLES20.glFinish()
    }

    /**
     * glReadPixels を呼び出す。
     * HDR の場合は RGB で 10bit 使う。
     *
     * [drawEnd]の後、[AkariGraphicsInputSurface.swapBuffers]の前に呼び出す必要がありそう？
     * （swapBuffers の後だと真っ暗だった）
     *
     * @return [GLES30.glReadPixels] の結果
     */
    internal fun glReadPixels(): ByteArray {
        // RGBA で 4バイト使う
        val byteArray = ByteArray(4 * height * width)
        val byteBuffer = ByteBuffer.wrap(byteArray)
        if (isEnableTenBitHdr) {
            // OpenGL ES の EGL で RGB 10 ビット、Alpha 2 ビット使っているので GL_UNSIGNED_INT_2_10_10_10_REV
            GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_INT_2_10_10_10_REV, byteBuffer)
        } else {
            GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, byteBuffer)
        }
        return byteArray
    }

    /** 破棄時に呼び出す */
    internal fun destroy() {
        // フレームバッファーオブジェクトの破棄
        // TODO 他のテクスチャも破棄しないといけない気がする
        fboPingPongManager?.getTextureIdList()?.forEach { textureId ->
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        }
        fboPingPongManager?.getFrameBufferObjectList()?.forEach { frameBuffer ->
            GLES20.glDeleteFramebuffers(1, intArrayOf(frameBuffer), 0)
        }
        hdrGlProgram?.destroy()
        sdrGlProgram?.destroy()
        canvasGlProgram?.destroy()
        fboGlProgram?.destroy()
        checkGlError("glDeleteTextures / glDeleteFramebuffers / glDeleteProgram")
    }

    private fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$op: glError $error")
        }
    }

    /**
     * [prepareDraw]、[applyEffect]、[drawEnd]の際にフレームバッファーオブジェクトを入れ替えるので
     *
     * [drawCanvas]と[drawSurfaceTexture]で呼び出さないのは、フレームバッファーオブジェクトのテクスチャを参照しないから。
     * 一方[applyEffect]は、[drawCanvas]や[drawSurfaceTexture]で書き込んだフレームバッファーオブジェクトを読み取って、
     * エフェクトを適用するので、描画先を入れ替える必要がある。
     *
     * [prepareDraw]は一応リセットを兼ねて（[GLES20.glClear]）、
     * [drawEnd]はフレームバッファーオブジェクトに書き込んだ内容を最後入れ替えて、画面に表示するため。
     *
     * また glUseProgram を中で呼び出しているため、関数の最初に呼び出すこと
     */
    private fun pingPongFrameBufferObject() {
        // フレームバッファーオブジェクトを入れ替え
        val nextFbo = fboPingPongManager?.pingPong() ?: return

        // 描画先をフレームバッファオブジェクトに
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, nextFbo.writeFrameBuffer)
        checkGlError("glBindFramebuffer")

        // フレームバッファーオブジェクトのテクスチャ指定
        // FBO 用に GLES20.GL_TEXTURE2
        fboGlProgram?.setActiveTexture(
            name = "sFboTexture",
            texId = nextFbo.readTextureId,
            target = GLES20.GL_TEXTURE_2D,
            texUnit = GLES20.GL_TEXTURE2,
            texUnitIndex = 2
        )
    }

    /**
     * フレームバッファオブジェクトの用意。やってる中身は grafika と同じ。
     * OpenGL ES の SurfaceView / MediaCodec のサイズと同じ大きさで FBO のテクスチャを作ります
     * [android.view.SurfaceHolder.setFixedSize]や[android.media.MediaFormat.KEY_WIDTH]参照
     *
     * @return フレームバッファーオブジェクトとテクスチャ ID
     */
    private fun generateFrameBufferObject(): FrameBufferObject {
        // フレームバッファオブジェクトの保存先になるテクスチャを作成
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val fboTextureId = textures.first()
        checkGlError("fbo glGenTextures")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
        checkGlError("fbo glActiveTexture glBindTexture")

        // テクスチャ ストレージの作成
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)

        // テクスチャの補完とか
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        checkGlError("fbo glTexParameter")

        // フレームバッファオブジェクトを作り、テクスチャをバインドする
        val frameBuffers = IntArray(1)
        GLES20.glGenFramebuffers(1, frameBuffers, 0)
        checkGlError("fbo glGenFramebuffers")
        val framebuffer = frameBuffers.first()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        checkGlError("fbo glBindFramebuffer ")

        // 深度バッファを作りバインドする
        val depthBuffers = IntArray(1)
        GLES20.glGenRenderbuffers(1, depthBuffers, 0)
        checkGlError("fbo glGenRenderbuffers")
        val depthBuffer = depthBuffers.first()
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthBuffer)
        checkGlError("fbo glBindRenderbuffer")

        // 深度バッファ用のストレージを作る
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height)
        checkGlError("fbo glRenderbufferStorage")

        // 深度バッファとテクスチャ (カラーバッファ) をフレームバッファオブジェクトにアタッチする
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthBuffer)
        checkGlError("fbo glFramebufferRenderbuffer")
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTextureId, 0)
        checkGlError("fbo glFramebufferTexture2D")

        // 完了したか確認
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer not complete, status = $status")
        }

        // デフォルトのフレームバッファに戻す
        // 描画の際には glBindFramebuffer で FBO に描画できる
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        // 返す
        return FrameBufferObject(textureId = fboTextureId, frameBuffer = framebuffer)
    }

    /** 16進数を RGBA の配列にする。それぞれ 0から1 */
    private fun Int.toColorVec4(): FloatArray {
        val r = (this shr 16 and 0xff) / 255.0f
        val g = (this shr 8 and 0xff) / 255.0f
        val b = (this and 0xff) / 255.0f
        val a = (this shr 24 and 0xff) / 255.0f
        return floatArrayOf(r, g, b, a)
    }

    /**
     * 2つのフレームバッファーオブジェクトを交互に使うやつ
     * [pingPong]で交互に取得できます。
     *
     * @param fbo1 ひとつめ
     * @param fbo2 ふたつめ
     */
    private class FboPingPongManager(
        private val fbo1: FrameBufferObject,
        private val fbo2: FrameBufferObject
    ) {

        private var first = true

        /**
         * 交互に FBO のテクスチャ ID を取得する
         * @return [NextFbo]
         */
        fun pingPong(): NextFbo {
            // 交互にする
            // フレームバッファーオブジェクトに書き込むとテクスチャとしてフラグメントシェーダーから利用できる
            val nextFbo = if (first) {
                NextFbo(fbo1.textureId, fbo2.frameBuffer)
            } else {
                NextFbo(fbo2.textureId, fbo1.frameBuffer)
            }
            first = !first
            return nextFbo
        }

        /** [fbo1]、[fbo2]のテクスチャ ID を返す。破棄用 */
        fun getTextureIdList(): List<Int> {
            return listOf(fbo1.textureId, fbo2.textureId)
        }

        /** [fbo1]、[fbo2]のフレームバッファーオブジェクトを返す。破棄用 */
        fun getFrameBufferObjectList(): List<Int> {
            return listOf(fbo1.frameBuffer, fbo2.frameBuffer)
        }
    }

    /**
     * ピンポンした FBO
     *
     * @param readTextureId [GLES20.glBindTexture]して、フラグメントシェーダーから FBO を読み出す
     * @param writeFrameBuffer [GLES20.glBindFramebuffer]して、描画内容を FBO に書き込む
     */
    private data class NextFbo(
        val readTextureId: Int,
        val writeFrameBuffer: Int
    )

    /**
     * フレームバッファーオブジェクト
     *
     * @param textureId 紐付けしたテクスチャ ID
     * @param frameBuffer 紐付けしたフレームバッファーオブジェクト
     */
    private data class FrameBufferObject(
        val textureId: Int,
        val frameBuffer: Int
    )

    companion object {

        /** [drawSurfaceTexture]でテクスチャの更新を待つ場合の、デフォルトタイムアウト */
        const val SURFACE_TEXTURE_UPDATE_TIMEOUT_MS = 1_000L

        private const val FLOAT_SIZE_BYTES = 4
        private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

        private val mTriangleVerticesData = floatArrayOf(
            -1.0f, -1.0f, 0f, 0f, 0f,
            1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f, 1.0f, 0f, 0f, 1f,
            1.0f, 1.0f, 0f, 1f, 1f
        )

        /** バーテックスシェーダー。vTextureCoord でフラグメントシェーダーへテクスチャ座標を渡します */
        private const val VERTEX_SHADER = """#version 300 es
in vec4 aPosition;
in vec4 aTextureCoord;

uniform mat4 uMVPMatrix;
uniform mat4 uSTMatrix;

out vec2 vTextureCoord;

void main() {
  gl_Position = uMVPMatrix * aPosition;
  vTextureCoord = (uSTMatrix * aTextureCoord).xy;
}
"""

        /**
         * 10Bit HDR 動画のフレームを描画するときに使うフラグメントシェーダー
         *
         * CameraX いわく
         * HDR 動画の場合は GL_EXT_YUV_target を使うべきらしい。
         * SDR のときの samplerExternalOES でも動くには動くらしいが、YUV の方が良いらしい
         * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:camera/camera-core/src/main/java/androidx/camera/core/processing/util/GLUtils.java;l=92
         */
        private val FRAGMENT_SHADER_10BIT_HDR_YUV = """#version 300 es
#extension GL_EXT_YUV_target : require
precision mediump float;

in vec2 vTextureCoord;
uniform __samplerExternal2DY2YEXT sSurfaceTexture;

uniform float chromakeyThreshold; // クロマキーのしきい値。0 でクロマキー無効
uniform vec4 chromakeyColor; // クロマキーにする色

out vec4 FragColor; // 出力色

// https://github.com/android/camera-samples/blob/a07d5f1667b1c022dac2538d1f553df20016d89c/Camera2Video/app/src/main/java/com/example/android/camera2/video/HardwarePipeline.kt#L107
vec3 yuvToRgb(vec3 yuv) {
  const vec3 yuvOffset = vec3(0.0625, 0.5, 0.5);
  const mat3 yuvToRgbColorTransform = mat3(
    1.1689f, 1.1689f, 1.1689f,
    0.0000f, -0.1881f, 2.1502f,
    1.6853f, -0.6530f, 0.0000f
  );
  return clamp(yuvToRgbColorTransform * (yuv - yuvOffset), 0.0, 1.0);
}

void main() {
    vec3 yuv = texture(sSurfaceTexture, vTextureCoord).xyz;
    vec3 rgb = yuvToRgb(yuv);
    
    // クロマキーで透過判定になったら discard
    if (chromakeyThreshold != .0 && length(rgb.rgb - chromakeyColor.rgb) < chromakeyThreshold) {
        discard;
    }
    
    FragColor = vec4(rgb, 1.0);
}
""".trimIndent()

        /** SDR 動画の時に使うフラグメントシェーダー */
        private val FRAGMENT_SHADER_SDR = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 vTextureCoord;
uniform samplerExternalOES sSurfaceTexture;

uniform float chromakeyThreshold; // クロマキーのしきい値。0 でクロマキー無効
uniform vec4 chromakeyColor; // クロマキーにする色

out vec4 FragColor; // 出力色

void main() {
    vec4 color = texture(sSurfaceTexture, vTextureCoord);
    
    // クロマキーで透過判定になったら discard
    if (chromakeyThreshold != .0 && length(color.rgb - chromakeyColor.rgb) < chromakeyThreshold) {
        discard;
    }
    
    FragColor = color;
}
""".trimIndent()

        /** Bitmap に書いた Canvas を描画するときに使うフラグメントシェーダー */
        private val FRAGMENT_SHADER_CANVAS_BITMAP = """#version 300 es
precision mediump float;

in vec2 vTextureCoord;
uniform sampler2D sCanvasTexture;

out vec4 FragColor; // 出力色

void main() {
    // テクスチャ座標なので Y を反転
    FragColor = texture(sCanvasTexture, vec2(vTextureCoord.x, 1.0 - vTextureCoord.y));
}
""".trimIndent()

        /** FBO に描画された内容を描画するときに使うフラグメントシェーダー */
        private val FRAGMENT_SHADER_FBO = """#version 300 es
precision mediump float;
in vec2 vTextureCoord;
uniform sampler2D sFboTexture;

out vec4 FragColor; // 出力色

void main() {
    FragColor = texture(sFboTexture, vTextureCoord);
}
""".trimIndent()

    }

}