package io.github.takusan23.akaricore.video.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * [io.github.takusan23.akaricore.video.CanvasVideoProcessor]用の[TextureRenderer]
 *
 * @param outputVideoWidth 動画の幅
 * @param outputVideoHeight 動画の高さ
 */
internal class CanvasRenderer(
    private val outputVideoWidth: Int,
    private val outputVideoHeight: Int
) : TextureRenderer() {

    private var mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.size * FLOAT_SIZE_BYTES).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(mTriangleVerticesData)
            position(0)
        }
    }

    private val mMVPMatrix = FloatArray(16)
    private val mSTMatrix = FloatArray(16)

    /** Canvasで書いたBitmap。Canvasの内容をOpenGLのテクスチャとして利用 */
    private val canvasBitmap by lazy { Bitmap.createBitmap(outputVideoWidth, outputVideoHeight, Bitmap.Config.ARGB_8888) }

    /** Canvas。これがエンコーダーに行く */
    private val canvas by lazy { Canvas(canvasBitmap) }

    // ハンドルたち
    private var mProgram = 0
    private var muMVPMatrixHandle = 0
    private var muSTMatrixHandle = 0
    private var maPositionHandle = 0
    private var maTextureHandle = 0
    private var uCanvasTextureHandle = 0

    /** キャンバスの画像を渡すOpenGLのテクスチャID */
    private var canvasTextureID = -1

    init {
        Matrix.setIdentityM(mSTMatrix, 0)
    }

    override fun createRenderer() {
        // シェーダーのコンパイル
        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (mProgram == 0) {
            throw RuntimeException("failed creating program")
        }

        // Uniform 変数へのハンドル
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition")
        checkGlError("glGetAttribLocation aPosition")
        if (maPositionHandle == -1) {
            throw RuntimeException("Could not get attrib location for aPosition")
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord")
        checkGlError("glGetAttribLocation aTextureCoord")
        if (maTextureHandle == -1) {
            throw RuntimeException("Could not get attrib location for aTextureCoord")
        }
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        checkGlError("glGetUniformLocation uMVPMatrix")
        if (muMVPMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uMVPMatrix")
        }
        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix")
        checkGlError("glGetUniformLocation uSTMatrix")
        if (muSTMatrixHandle == -1) {
            throw RuntimeException("Could not get attrib location for uSTMatrix")
        }
        uCanvasTextureHandle = GLES20.glGetUniformLocation(mProgram, "uCanvasTexture")
        checkGlError("glGetUniformLocation uCanvasTexture")
        if (uCanvasTextureHandle == -1) {
            throw RuntimeException("Could not get attrib location for uCanvasTexture")
        }

        // Canvas のテクスチャID を払い出してもらう
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        // Canvasテクスチャ
        canvasTextureID = textures[0]
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, canvasTextureID)
        checkGlError("glBindTexture canvasTextureID")

        // 縮小拡大時の補間設定
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // テクスチャを初期化
        // 更新の際はコンテキストを切り替えた上で texSubImage2D を使う
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, canvasBitmap, 0)
        checkGlError("glTexParameter canvasTextureID")
    }

    /**
     * 描画する
     *
     * @param onCanvasDrawRequest Canvas を渡すので、フレームを描画してください
     */
    suspend fun draw(onCanvasDrawRequest: suspend (Canvas) -> Unit) {
        prepareDraw()
        requestDraw(onCanvasDrawRequest)
        GLES20.glFinish()
    }

    override fun destroy() {
        // do nothing
    }

    private fun prepareDraw() {
        // glError 1282 の原因とかになる
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices)
        checkGlError("glVertexAttribPointer maPosition")
        GLES20.glEnableVertexAttribArray(maPositionHandle)
        checkGlError("glEnableVertexAttribArray maPositionHandle")
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices)
        checkGlError("glVertexAttribPointer maTextureHandle")
        GLES20.glEnableVertexAttribArray(maTextureHandle)
        checkGlError("glEnableVertexAttribArray maTextureHandle")

        // Snapdragon だと glClear が無いと映像が乱れる
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
    }

    private suspend fun requestDraw(onCanvasDrawRequest: suspend (Canvas) -> Unit) {
        // コンテキストをCanvasのテクスチャIDに切り替える
        // テクスチャ設定
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, canvasTextureID)

        // 縮小拡大時の補間設定
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // Canvas 前回のを消す
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        // Canvasで書く
        onCanvasDrawRequest(canvas)

        // glActiveTexture したテクスチャへCanvasで書いた画像を転送する
        // 更新なので texSubImage2D
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, canvasBitmap)
        checkGlError("GLUtils.texSubImage2D canvasTextureID")

        // Uniform 変数へテクスチャを設定
        // 第二引数は GLES20.GL_TEXTURE0 なので 0
        GLES20.glUniform1i(uCanvasTextureHandle, 0)
        checkGlError("glUniform1i uCanvasTextureHandle")

        // アスペクト比の調整はいらないのでリセット（エンコーダーの出力サイズにCanvasを合わせて作っているため）
        Matrix.setIdentityM(mMVPMatrix, 0)
        // それとは別に、OpenGLの画像は原点が左下なので（普通は左上）、行列を反転させる
        // すいませんよくわかりません。
        Matrix.setIdentityM(mSTMatrix, 0)
        Matrix.scaleM(mSTMatrix, 0, 1f, -1f, 1f)

        // 描画する
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0)
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays Canvas")
    }

    companion object {

        private val mTriangleVerticesData = floatArrayOf(
            -1.0f, -1.0f, 0f, 0f, 0f,
            1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f, 1.0f, 0f, 0f, 1f,
            1.0f, 1.0f, 0f, 1f, 1f
        )

        private const val FLOAT_SIZE_BYTES = 4
        private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

        /** バーテックスシェーダー。座標などを決める */
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uSTMatrix;
            attribute vec4 aPosition;
            attribute vec4 aTextureCoord;
            varying vec2 vTextureCoord;
            
            void main() {
              gl_Position = uMVPMatrix * aPosition;
              vTextureCoord = (uSTMatrix * aTextureCoord).xy;
            }
        """

        /** フラグメントシェーダー。実際の色を返す */
        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require

            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D uCanvasTexture;
        
            void main() {
                gl_FragColor = texture2D(uCanvasTexture, vTextureCoord);
            }
        """
    }
}