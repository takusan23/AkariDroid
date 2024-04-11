package io.github.takusan23.akaricore.video.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * [io.github.takusan23.akaricore.video.VideoFrameBitmapExtractor]用[TextureRenderer]
 *
 * # クロマキーで透過する機能
 * BB素材を透過できます。CPU ではなく、OpenGL ES（GPU）で処理させるため高速です。
 * 透過させるかどうかの条件式は以下のとおりです。
 *
 * if (length(クロマキーの色.rgba - テクスチャの色.rgba) < しきい値)
 *
 * クロマキー機能を使わない場合は両方 null でいいよ。
 * 参考： https://wgld.org/d/webgl/w080.html
 *
 * @param chromakeyThreshold クロマキーのしきい値。
 * @param chromakeyColor クロマキーの色。しきい値を考慮するので、近しい色も透過するはず。
 */
class FrameExtractorRenderer(
    private val chromakeyThreshold: Float? = null,
    private val chromakeyColor: Int? = null
) : TextureRenderer() {

    private val mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.size * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val mMVPMatrix = FloatArray(16)
    private val mSTMatrix = FloatArray(16)
    private var mProgram = 0
    private var muMVPMatrixHandle = 0
    private var muSTMatrixHandle = 0
    private var maPositionHandle = 0
    private var maTextureHandle = 0

    /**
     * 新しいフレームが来ているか。
     * [SurfaceTexture.OnFrameAvailableListener]が呼ばれたか。
     *
     * 他スレッドからも参照するので、[MutableStateFlow]等のスレッドセーフである必要があります。
     */
    private val isAvailableNewFrameFlow = MutableStateFlow(false)

    // MediaCodec でデコードしたフレームをテクスチャで
    private var textureId = -1234567
    private var surfaceTexture: SurfaceTexture? = null

    // クロマキー
    private var uChromakeyThresholdHandle = 0
    private var uChromakeyColorHandle = 0

    /** MediaCodec の出力先としてこれを使う。MediaCodec で受け取って OpenGL ES で描画するため */
    var inputSurface: Surface? = null
        private set

    init {
        mTriangleVertices.put(mTriangleVerticesData).position(0)
        Matrix.setIdentityM(mSTMatrix, 0)
    }

    override fun createRenderer() {
        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (mProgram == 0) {
            throw RuntimeException("failed creating program")
        }
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
        uChromakeyThresholdHandle = GLES20.glGetUniformLocation(mProgram, "uChromakeyThreshold")
        checkGlError("glGetUniformLocation uChromakeyThresholdHandle")
        if (uChromakeyThresholdHandle == -1) {
            throw RuntimeException("Could not get attrib location for uChromakeyThreshold")
        }
        uChromakeyColorHandle = GLES20.glGetUniformLocation(mProgram, "uChromakeyColor")
        checkGlError("glGetUniformLocation uChromakeyColorHandle")
        if (uChromakeyColorHandle == -1) {
            throw RuntimeException("Could not get attrib location for uChromakeyColor")
        }

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        checkGlError("glBindTexture mTextureID")

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        checkGlError("glTexParameter")

        Matrix.setIdentityM(mMVPMatrix, 0)

        // glGenTextures で作ったテクスチャは SurfaceTexture で使う
        surfaceTexture = SurfaceTexture(textureId)
        inputSurface = Surface(surfaceTexture)

        // 描画する
        surfaceTexture?.setOnFrameAvailableListener {
            // StateFlow はスレッドセーフらしいので同期処理は入れない
            isAvailableNewFrameFlow.value = true
        }
    }

    /** 16進数を RGBA の配列にする。それぞれ 0から1 */
    private fun Int.toColorVec4(): FloatArray {
        val r = (this shr 16 and 0xff) / 255.0f
        val g = (this shr 8 and 0xff) / 255.0f
        val b = (this and 0xff) / 255.0f
        val a = (this shr 24 and 0xff) / 255.0f
        return floatArrayOf(r, g, b, a)
    }

    override fun destroy() {
        surfaceTexture?.release()
        inputSurface?.release()
    }

    /** 描画する */
    suspend fun draw() {
        // 新しいフレームが来るまで待つ
        // true になるのを待つ
        isAvailableNewFrameFlow.first { it }
        // テクスチャを更新して、フラグを折る
        surfaceTexture?.updateTexImage()
        isAvailableNewFrameFlow.value = false
        // 描画する
        checkGlError("onDrawFrame start")
        surfaceTexture?.getTransformMatrix(mSTMatrix)
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
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

        // クロマキー用
        // null の場合は 0 にして動かないように
        GLES20.glUniform1f(uChromakeyThresholdHandle, chromakeyThreshold ?: 0f)
        GLES20.glUniform4fv(uChromakeyColorHandle, 1, chromakeyColor?.toColorVec4() ?: floatArrayOf(0f, 0f, 0f, 0f), 0)
        checkGlError("glUniform1f glUniform4fv")

        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0)
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        GLES20.glFinish()
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
        private const val FRAGMENT_SHADER = """
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;

uniform float uChromakeyThreshold;
uniform vec4 uChromakeyColor;

void main() {
  
  if (uChromakeyThreshold == .0) {
    // クロマキーしない場合
    gl_FragColor = texture2D(sTexture, vTextureCoord);  
  } else {
    // クロマキーで透過する
    vec4 textureColor = texture2D(sTexture, vTextureCoord);
    float diff = length(uChromakeyColor - textureColor);
    
    // しきい値まで見る
    if (diff < uChromakeyThreshold) {
        discard;
    } else {
        gl_FragColor = textureColor;        
    }
  }
}
"""
    }

}