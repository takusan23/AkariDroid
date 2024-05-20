package io.github.takusan23.akaricore.video.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @param fragmentShaderCode フラグメントシェーダー
 */
class ShaderImageRenderer(
    private val fragmentShaderCode: String
) : TextureRenderer() {

    private val mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.size * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var mProgram = 0
    private var maPositionHandle = 0
    private var maTextureHandle = 0
    private var sTextureHandle = 0

    // 渡された Bitmap をテクスチャとして使うので、ユニット番号
    private var textureId = -1234567

    init {
        mTriangleVertices.put(mTriangleVerticesData).position(0)
    }

    override fun createRenderer() {
        // シェーダーのコンパイル
        mProgram = createProgram(VERTEX_SHADER, fragmentShaderCode)
        if (mProgram == 0) {
            throw RuntimeException("failed creating program")
        }

        // Uniform 変数へのハンドル
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_position")
        checkGlError("glGetAttribLocation maPositionHandle")
        if (maPositionHandle == -1) {
            throw RuntimeException("Could not get attrib location for maPositionHandle")
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "a_textureCoord")
        checkGlError("glGetAttribLocation maTextureHandle")
        if (maTextureHandle == -1) {
            throw RuntimeException("Could not get attrib location for maTextureHandle")
        }
        sTextureHandle = GLES20.glGetUniformLocation(mProgram, "s_texture")
        checkGlError("glGetUniformLocation sTextureHandle")
        if (sTextureHandle == -1) {
            throw RuntimeException("Could not get attrib location for sTextureHandle")
        }

        // テクスチャ ID を払い出してもらう
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)

        // バインドする
        textureId = textures[0]
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        checkGlError("glBindTexture textureId")

        // 縮小拡大時の補間設定
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    override fun destroy() {
        // do nothing
    }

    /**
     * Bitmap を受け取って、OpenGL ES 側でフラグメントシェーダーを使い、描画する
     *
     * @param bitmap [Bitmap]
     */
    fun draw(bitmap: Bitmap) {
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

        // 画像を渡す
        // texImage2D、引数違いがいるので注意
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        checkGlError("GLUtils.texImage2D")

        // Uniform 変数へテクスチャを設定
        // 第二引数は GLES20.GL_TEXTURE0 なので 0
        GLES20.glUniform1i(sTextureHandle, 0)
        checkGlError("glUniform1i sTextureHandle")

        // 描画する
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        GLES20.glFinish()
        checkGlError("glFinish")
    }

    companion object {
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

        private const val VERTEX_SHADER = """
attribute vec4 a_position;
attribute vec4 a_textureCoord;
varying vec2 v_textureCoord;

void main() {
  gl_Position = a_position;
  v_textureCoord = a_textureCoord.xy;
  v_textureCoord.y = 1.-v_textureCoord.y;
}
"""

        // TODO 時間と height/width を取るようにしたい
        internal const val DEMO_FRAGMENT_SHADER = """
precision mediump float;
varying vec2 v_textureCoord;
uniform sampler2D s_texture;

void main() {
  vec4 color = texture2D(s_texture, v_textureCoord);
  // color.g = 0.0;
  // color.b = 0.0;
  gl_FragColor = color;
}
"""
    }
}