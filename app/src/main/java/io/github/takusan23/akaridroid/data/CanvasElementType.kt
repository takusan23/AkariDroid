package io.github.takusan23.akaridroid.data

/** 描画するタイプ */
sealed class CanvasElementType {

    /**
     * テキストを描画する
     *
     * @param text テキスト
     * @param color 文字色
     * @param fontSize フォントサイズ
     */
    data class TextElement(val text: String, val color: Int, val fontSize: Float) : CanvasElementType()

}