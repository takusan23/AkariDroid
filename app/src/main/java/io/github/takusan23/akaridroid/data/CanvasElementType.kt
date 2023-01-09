package io.github.takusan23.akaridroid.data

import kotlinx.serialization.Serializable

/** 描画するタイプ */
@Serializable
sealed class CanvasElementType {

    /**
     * テキストを描画する
     *
     * @param text テキスト
     * @param color 文字色
     * @param fontSize フォントサイズ
     */
    @Serializable
    data class TextElement(val text: String, val color: Int, val fontSize: Float) : CanvasElementType()

}