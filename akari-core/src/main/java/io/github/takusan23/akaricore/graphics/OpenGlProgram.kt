package io.github.takusan23.akaricore.graphics

import android.opengl.GLES20

/**
 * OpenGL ES のプログラムを管理するクラス。シェーダーのコンパイルとか。
 * 複数のプログラム（フラグメントシェーダー）をいい感じに切り替えるためのクラス。uniform とかを閉じ込めた。
 * GL スレッドから呼び出すこと
 *
 * @param vertexShaderCode バーテックスシェーダーのコード
 * @param fragmentShaderCode フラグメントシェーダーのコード
 */
internal class OpenGlProgram(
    private val vertexShaderCode: String,
    private val fragmentShaderCode: String
) {
    private var mProgram = 0

    /** uniform float 変数のロケーションの連想配列。キー名が uniform 変数名で値は getUniformLocation */
    private val floatUniformLocationMap = hashMapOf<String, Int>()

    /** uniform vec2 変数のロケーションの連想配列。キー名が uniform 変数名で値は getUniformLocation */
    private val vec2UniformLocationMap = hashMapOf<String, Int>()

    /** uniform vec4 変数のロケーションの連想配列。キー名が uniform 変数名で値は getUniformLocation */
    private val vec4UniformLocationMap = hashMapOf<String, Int>()

    /** attribute 変数のロケーションの連想配列。キー名が attribute 変数名で値は glGetAttribLocation */
    private val attributeLocationMap = hashMapOf<String, Int>()

    /** テクスチャの uniform 変数のロケーションの連想配列。キー名が uniform 変数名で値は getUniformLocation */
    private val textureUniformLocationMap = hashMapOf<String, Int>()

    /** uniform mat4 変数のロケーションの連想配列。キー名が uniform 変数名で値は getUniformLocation */
    private val mat4UniformLocationMap = hashMapOf<String, Int>()

    /** コンパイルを行う */
    fun prepare() {
        mProgram = createProgram(
            vertexSource = vertexShaderCode,
            fragmentSource = fragmentShaderCode
        )
        if (mProgram == 0) {
            throw RuntimeException("failed creating program")
        }
    }

    /**
     * この OpenGL ES プログラム（バーテックスシェーダー、フラグメントシェーダー）を使う。
     * 描画コマンドは呼び出し側で...
     *
     * 呼び出さないと glError 1282 の原因とかになる
     */
    fun use() {
        GLES20.glUseProgram(mProgram)
        checkGlError("glUseProgram")
    }

    /** 破棄する */
    fun destroy() {
        GLES20.glDeleteProgram(mProgram)
        mProgram = 0
    }

    /**
     * uniform float 変数を登録する
     * [setFloatUniform]で登録できる
     *
     * @param name uniform float 変数名
     */
    fun registerFloatUniformLocation(name: String) {
        floatUniformLocationMap[name] = GLES20.glGetUniformLocation(mProgram, name)
        checkGlError("glGetUniformLocation $name")
    }

    /**
     * uniform vec2 変数のロケーションを登録する
     * [setVec2Uniform]で登録できる
     *
     * @param name uniform vec2 変数名
     */
    fun registerVec2UniformLocation(name: String) {
        vec2UniformLocationMap[name] = GLES20.glGetUniformLocation(mProgram, name)
        checkGlError("glGetUniformLocation $name")
    }

    /**
     * uniform vec4 変数のロケーションを登録する
     * [setVec4Uniform]で登録できる
     *
     * @param name uniform vec4 変数名
     */
    fun registerVec4UniformLocation(name: String) {
        vec4UniformLocationMap[name] = GLES20.glGetUniformLocation(mProgram, name)
        checkGlError("glGetUniformLocation $name")
    }

    /**
     * attribute 変数（in パラメータ）のロケーションを登録する
     * [setVertexAttribute]で登録できる
     *
     * @param name attribute 変数名
     */
    fun registerAttributeLocation(name: String) {
        attributeLocationMap[name] = GLES20.glGetAttribLocation(mProgram, name)
        checkGlError("glGetAttribLocation $name")
    }

    /**
     * uniform テクスチャのロケーションを登録する
     * [setActiveTexture]で登録できる
     *
     * @param name uniform テクスチャ変数名
     */
    fun registerTextureUniformLocation(name: String) {
        textureUniformLocationMap[name] = GLES20.glGetUniformLocation(mProgram, name)
        checkGlError("glGetUniformLocation $name")
    }

    /**
     * uniform mat4 変数のロケーションを登録する
     * [setMat4Uniform]で登録できる
     *
     * @param name uniform mat4 変数名
     */
    fun registerMat4UniformLocation(name: String) {
        mat4UniformLocationMap[name] = GLES20.glGetUniformLocation(mProgram, name)
        checkGlError("glGetUniformLocation $name")
    }

    /**
     * uniform float 変数に値をセットする
     *
     * @param name uniform float 変数名
     * @param value セットする値
     */
    fun setFloatUniform(name: String, value: Float) {
        use()
        val location = floatUniformLocationMap[name] ?: return
        GLES20.glUniform1f(location, value)
        checkGlError("glUniform1f $name")
    }

    /**
     * uniform vec2 変数に値をセットする
     *
     * @param name uniform vec2 変数名
     * @param float1 vec2.x
     * @param float2 vec2.y
     */
    fun setVec2Uniform(name: String, float1: Float, float2: Float) {
        use()
        val location = vec2UniformLocationMap[name] ?: return
        GLES20.glUniform2f(location, float1, float2)
        checkGlError("glUniform2f $name")
    }

    /**
     * uniform vec4 変数に値をセットする
     *
     * @param name uniform vec4 変数名
     * @param float1 vec4.x
     * @param float2 vec4.y
     * @param float3 vec4.z
     * @param float4 vec4.w
     */
    fun setVec4Uniform(name: String, float1: Float, float2: Float, float3: Float, float4: Float) {
        use()
        val location = vec4UniformLocationMap[name] ?: return
        GLES20.glUniform4f(location, float1, float2, float3, float4)
        checkGlError("glUniform4f $name")
    }

    /**
     * attribute 変数（in パラメータ）に値をセットする
     *
     * @param name attribute 変数名
     * @param size attribute glVertexAttribPointer の size
     * @param type attribute glVertexAttribPointer の type
     * @param normalized attribute glVertexAttribPointer の normalized
     * @param stride attribute glVertexAttribPointer の stride
     * @param pointer attribute glVertexAttribPointer の pointer
     */
    fun setVertexAttribute(name: String, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: java.nio.Buffer) {
        use()
        val location = attributeLocationMap[name] ?: return
        GLES20.glVertexAttribPointer(location, size, type, normalized, stride, pointer)
        checkGlError("glVertexAttribPointer $name")
        GLES20.glEnableVertexAttribArray(location)
        checkGlError("glEnableVertexAttribArray $name")
    }

    /**
     * uniform mat4 変数に値をセットする
     *
     * @param name uniform mat4 変数名
     * @param matrix セットする 4x4 行列（float[16]）
     */
    fun setMat4Uniform(name: String, matrix: FloatArray) {
        use()
        val location = mat4UniformLocationMap[name] ?: return
        GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0)
        checkGlError("glUniformMatrix4fv $name")
    }

    /**
     * テクスチャを有効にする
     *
     * @param name uniform テクスチャ変数名
     * @param texId テクスチャ ID（GLES20.glGenTextures したもの）
     * @param target テクスチャのターゲット（GLES20.GL_TEXTURE_2D や GLES11Ext.GL_TEXTURE_EXTERNAL_OES など）
     * @param texUnit テクスチャユニット（GLES20.GL_TEXTURE1 みたいな）
     * @param texUnitIndex GLES20.GL_TEXTURE0 なら 0、GLES20.GL_TEXTURE1 なら 1
     */
    fun setActiveTexture(name: String, texId: Int, target: Int, texUnit: Int, texUnitIndex: Int) {
        use()
        val location = textureUniformLocationMap[name] ?: return
        GLES20.glActiveTexture(texUnit)
        checkGlError("glActiveTexture $name")
        GLES20.glBindTexture(target, texId)
        checkGlError("glBindTexture $name")
        GLES20.glUniform1i(location, texUnitIndex)
        checkGlError("glUniform1i $name")
    }

    /**
     * GLSL（フラグメントシェーダー・バーテックスシェーダー）をコンパイルして、OpenGL ES とリンクする
     *
     * @throws GlslSyntaxErrorException 構文エラーの場合に投げる
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
     * @throws GlslSyntaxErrorException 構文エラーの場合に投げる
     * @throws RuntimeException それ以外
     * @return 0 以外で成功
     */
    private fun loadShader(shaderType: Int, source: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
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
}