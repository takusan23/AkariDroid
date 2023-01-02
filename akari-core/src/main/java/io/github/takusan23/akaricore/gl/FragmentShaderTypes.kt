package io.github.takusan23.akaricore.gl

/**
 * OpenGLのフラグメントシェーダー
 * エフェクトをかける際に使うGLSL言語のコードです
 *
 * @param fragmentShaderCode フラグメントシェーダー
 */
enum class FragmentShaderTypes(val fragmentShaderCode: String) {

    /** デフォルト */
    DEFAULT("""
        #extension GL_OES_EGL_image_external : require

        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;        
        
        void main() {
            gl_FragColor = texture2D(sTexture, vTextureCoord);
        }
    """.trimIndent()),

    /** 左右反転にするシェーダー */
    FLIP("""
        #extension GL_OES_EGL_image_external : require

        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;        
        
        void main() {
            gl_FragColor = texture2D(sTexture, vec2(1.0 - vTextureCoord.x, vTextureCoord.y));
        }
    """.trimIndent()),

    /** モノクロにするシェーダー */
    MONOCHROME("""
        #extension GL_OES_EGL_image_external : require
        
        #define R_LUMINANCE 0.298912
        #define G_LUMINANCE 0.586611
        #define B_LUMINANCE 0.114478
        const vec3 monochromeScale = vec3(R_LUMINANCE, G_LUMINANCE, B_LUMINANCE);

        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;        
        
        void main() {
            vec4 color = texture2D(sTexture, vTextureCoord);
            float grayColor = dot(color.rgb, monochromeScale);
            color = vec4(vec3(grayColor), 1.0);
            gl_FragColor = vec4(color);
        }
    """.trimIndent()),

    /** モザイクをかけるシェーダー */
    MOSAIC("""
        #extension GL_OES_EGL_image_external : require

        precision mediump float;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;        
        
        void main() {
            vec2 uv = vTextureCoord;
            uv = floor(uv * 30.0) / 30.0;
            vec4 color = texture2D(sTexture, uv);
            gl_FragColor = vec4(color.rgb, 1.0);
        }
    """.trimIndent())

}