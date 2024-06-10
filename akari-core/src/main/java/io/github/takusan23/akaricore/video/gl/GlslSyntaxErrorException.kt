package io.github.takusan23.akaricore.video.gl

/**
 * [TextureRenderer]で、GLSL の構文エラー（変数が定義されて無い、セミコロンが無い）があった際に投げる例外。
 * それ以外の OpenGL ES のエラーは[TextureRenderer.checkGlError]の通りです。
 *
 * @param syntaxErrorMessage 構文エラーのメッセージ
 */
data class GlslSyntaxErrorException(
    val syntaxErrorMessage: String
) : RuntimeException()