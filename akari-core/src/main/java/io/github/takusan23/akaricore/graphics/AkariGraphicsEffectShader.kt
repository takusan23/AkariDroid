package io.github.takusan23.akaricore.graphics

import android.opengl.GLES20
import io.github.takusan23.akaricore.graphics.AkariGraphicsEffectShader.Companion.VERTEX_SHADER_GLSL100
import io.github.takusan23.akaricore.graphics.AkariGraphicsEffectShader.Companion.VERTEX_SHADER_GLSL300
import io.github.takusan23.akaricore.video.gl.GlslSyntaxErrorException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * フラグメントシェーダーでフレームにエフェクトを適用する。
 * [AkariGraphicsProcessor]は最後以外は描画先がフレームバッファオブジェクトになるので、そのテクスチャにエフェクトを適用する。
 *
 * [prepareShader]を呼び出してから、[AkariGraphicsTextureRenderer.applyEffect]を呼び出す。
 *
 * # フラグメントシェーダーで使える uniform 変数
 * ## uniform vec2 vResolution
 * 出力先の縦横サイズが入っています。座標の正規化に使います。
 *
 * ## uniform sampler2D sVideoFrameTexture
 * フレームバッファオブジェクトのテクスチャです。
 * texture() に入れて使います。
 *
 * @param vertexShaderCode "#version 100"の場合は[VERTEX_SHADER_GLSL100]、"#version 300"の場合は[VERTEX_SHADER_GLSL300]
 * @param fragmentShaderCode フラグメントシェーダー。使える uniform 変数は上記です。
 */
class AkariGraphicsEffectShader(
    private val vertexShaderCode: String = VERTEX_SHADER_GLSL300,
    private val fragmentShaderCode: String = FRAGMENT_SHADER_NEGATIVE
) {

    private val mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.size * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var mProgram = 0
    private var maPositionHandle = 0

    // Uniform 変数のハンドル
    private var vResolutionHandle = 0
    private var sVideoFrameTextureHandle = 0

    // ユーザー追加 uniform 変数

    /** float 版。key は uniform 変数名、value は glGetUniformLocation の返り値 */
    private val floatUniformLocationMap = hashMapOf<String, Int>()

    /** vec2 版。key は uniform 変数名、value は glGetUniformLocation の返り値 */
    private val vec2UniformLocationMap = hashMapOf<String, Int>()

    /** vec4 版。key は uniform 変数名、value は glGetUniformLocation の返り値 */
    private val vec4UniformLocationMap = hashMapOf<String, Int>()

    init {
        mTriangleVertices.put(mTriangleVerticesData).position(0)
    }

    /** フラグメントシェーダーのコンパイル等を行う */
    fun prepareShader() {
        // シェーダーのコンパイル
        mProgram = createProgram(vertexShaderCode, fragmentShaderCode)
        if (mProgram == 0) {
            throw RuntimeException("failed creating program")
        }

        // Uniform 変数へのハンドル
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_position")
        checkGlError("glGetAttribLocation maPositionHandle")
        if (maPositionHandle == -1) {
            throw RuntimeException("Could not get attrib location for maPositionHandle")
        }
        vResolutionHandle = GLES20.glGetUniformLocation(mProgram, "vResolution")
        checkGlError("glGetUniformLocation vResolution")
        if (vResolutionHandle == -1) {
            throw RuntimeException("Could not get uniform location for vResolution")
        }
        sVideoFrameTextureHandle = GLES20.glGetUniformLocation(mProgram, "sVideoFrameTexture")
        checkGlError("glGetUniformLocation sVideoFrameTexture")
        if (sVideoFrameTextureHandle == -1) {
            throw RuntimeException("Could not get uniform location for sVideoFrameTexture")
        }

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        checkGlError("glEnable GLES20.GL_BLEND")
    }

    /**
     * uniform float 変数を探して登録する
     *
     * @param uniformName uniform 変数名
     */
    fun findFloatUniformLocation(uniformName: String) {
        floatUniformLocationMap[uniformName] = GLES20.glGetUniformLocation(mProgram, uniformName)
        checkGlError("glGetUniformLocation $uniformName")
    }

    /**
     * uniform vec2 変数を探して登録する
     *
     * @param uniformName uniform 変数名
     */
    fun findVec2UniformLocation(uniformName: String) {
        vec2UniformLocationMap[uniformName] = GLES20.glGetUniformLocation(mProgram, uniformName)
        checkGlError("glGetUniformLocation $uniformName")
    }

    /**
     * uniform vec4 変数を探して登録する
     *
     * @param uniformName uniform 変数名
     */
    fun findVec4UniformLocation(uniformName: String) {
        vec4UniformLocationMap[uniformName] = GLES20.glGetUniformLocation(mProgram, uniformName)
        checkGlError("glGetUniformLocation $uniformName")
    }

    /**
     * float uniform 変数に値をセットする
     *
     * @param uniformName uniform 変数名
     * @param float 値
     */
    fun setFloatUniform(uniformName: String, float: Float) {
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")
        val location = floatUniformLocationMap[uniformName] ?: return
        GLES20.glUniform1f(location, float)
        checkGlError("glUniform1f $uniformName")
    }

    /**
     * vec2 uniform 変数に値をセットする
     *
     * @param uniformName uniform 変数名
     * @param float1 vec2
     * @param float2 vec2
     */
    fun setVec2Uniform(uniformName: String, float1: Float, float2: Float) {
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")
        val location = vec2UniformLocationMap[uniformName] ?: return
        GLES20.glUniform2f(location, float1, float2)
        checkGlError("glUniform2f $uniformName")
    }

    /**
     * vec4 uniform 変数に値をセットする
     *
     * @param uniformName uniform 変数名
     * @param float1 vec4
     * @param float2 vec4
     * @param float3 vec4
     * @param float4 vec4
     */
    fun setVec4Uniform(uniformName: String, float1: Float, float2: Float, float3: Float, float4: Float) {
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")
        val location = vec4UniformLocationMap[uniformName] ?: return
        GLES20.glUniform4f(location, float1, float2, float3, float4)
        checkGlError("glUniform4f $uniformName")
    }

    /**
     * エフェクトを適用する
     *
     * @param width 幅
     * @param height 高さ
     * @param fboTextureUnit フレームバッファオブジェクトのテクスチャユニット
     */
    internal fun applyEffect(
        width: Int,
        height: Int,
        fboTextureUnit: Int
    ) {
        // glUseProgram する
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")

        // Uniform 変数に渡していく
        // テクスチャ ID
        GLES20.glUniform1i(sVideoFrameTextureHandle, fboTextureUnit)
        // 解像度
        GLES20.glUniform2f(vResolutionHandle, width.toFloat(), height.toFloat())

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices)
        checkGlError("glVertexAttribPointer maPosition")
        GLES20.glEnableVertexAttribArray(maPositionHandle)
        checkGlError("glEnableVertexAttribArray maPositionHandle")
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        // 描画する
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        checkGlError("glDrawArrays")
        GLES20.glFinish()
        checkGlError("glFinish")
    }

    /** 破棄時に呼び出す。GL スレッドから呼ばないとだめな気がします。 */
    fun destroy() {
        GLES20.glDeleteProgram(mProgram)
    }

    /**
     * GLSL（フラグメントシェーダー・バーテックスシェーダー）をコンパイルして、OpenGL ES とリンクする
     *
     * @throws RuntimeException それ以外
     * @return 0 以外で成功
     */
    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) {
            return 0
        }
        var program = GLES20.glCreateProgram()
        checkGlError("glCreateProgram")
        if (program == 0) {
            return 0
        }
        GLES20.glAttachShader(program, vertexShader)
        checkGlError("glAttachShader")
        GLES20.glAttachShader(program, pixelShader)
        checkGlError("glAttachShader")
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        return program
    }

    /**
     * GLSL（フラグメントシェーダー・バーテックスシェーダー）のコンパイルをする
     *
     * @throws RuntimeException それ以外
     * @return 0 以外で成功
     */
    private fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES20.glCreateShader(shaderType)
        checkGlError("glCreateShader type=$shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            // 失敗したら例外を投げる。その際に構文エラーのメッセージを取得する
            val syntaxErrorMessage = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw GlslSyntaxErrorException(syntaxErrorMessage)
            // ここで return 0 しても例外を投げるので意味がない
            // shader = 0
        }
        return shader
    }

    private fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$op: glError $error")
        }
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

        const val VERTEX_SHADER_GLSL300 = """#version 300 es
            in vec4 a_position;
            
            void main() {
              gl_Position = a_position;
            }
        """

        const val VERTEX_SHADER_GLSL100 = """#version 100
            attribute vec4 a_position;
            
            void main() {
              gl_Position = a_position;
            }
        """

        /** 色反転フラグメントシェーダー。サンプル用 */
        const val FRAGMENT_SHADER_NEGATIVE = """#version 300 es
            precision mediump float;

            uniform vec2 vResolution;
            uniform sampler2D sVideoFrameTexture;
            
            out vec4 FragColor;

            void main() {
              // テクスチャ座標に変換
              vec2 vTextureCoord = gl_FragCoord.xy / vResolution.xy;
              // 出力色
              vec4 outColor = vec4(1.);
              // 色暗転
              outColor = texture(sVideoFrameTexture, vTextureCoord);
              outColor.rgb = vec3(1.) - outColor.rgb;
              // 出力
              FragColor = outColor;
            }
        """
    }

}