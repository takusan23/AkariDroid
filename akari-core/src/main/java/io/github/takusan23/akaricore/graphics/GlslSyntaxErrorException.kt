package io.github.takusan23.akaricore.graphics

/**
 * [AkariGraphicsEffectShader]などで、GLSL の構文エラー（変数が定義されて無い、セミコロンが無い）があった際に投げる例外。
 *
 * @param syntaxErrorMessage 構文エラーのメッセージ
 */
data class GlslSyntaxErrorException(
    val syntaxErrorMessage: String
) : RuntimeException()