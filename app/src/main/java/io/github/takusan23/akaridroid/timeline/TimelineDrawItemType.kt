package io.github.takusan23.akaridroid.timeline

import kotlinx.serialization.Serializable

/** [TimelineItemData.CanvasData] で 描画するアイテム */
sealed interface TimelineDrawItemType {

    /**
     * テキストを描画する
     *
     * @param text テキスト
     * @param color 文字色
     * @param fontSize フォントサイズ
     */
    @Serializable
    data class TextItem(val text: String, val color: Int, val fontSize: Float) : TimelineDrawItemType

    /**
     * 図形（とりあえず四角形）
     *
     * @param width
     * @param height
     * @param color
     */
    @Serializable
    data class RectItem(val width: Float, val height: Float, val color: Int) : TimelineDrawItemType

}