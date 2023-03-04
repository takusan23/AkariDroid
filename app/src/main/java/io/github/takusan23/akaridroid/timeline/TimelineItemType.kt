package io.github.takusan23.akaridroid.timeline

import kotlinx.serialization.Serializable

/** 描画するアイテム */
sealed interface TimelineItemType {

    /**
     * テキストを描画する
     *
     * @param text テキスト
     * @param color 文字色
     * @param fontSize フォントサイズ
     */
    @Serializable
    data class TextItem(val text: String, val color: Int, val fontSize: Float) : TimelineItemType


    /**
     * 図形（とりあえず四角形）
     *
     * @param width
     * @param height
     * @param color
     */
    @Serializable
    data class RectItem(val width: Float, val height: Float, val color: Int) : TimelineItemType

    /**
     * 動画を描画する
     * （akari-core の制約上、同時に動画は一つだけ描画可能）
     *
     * TODO [TimelineItemData.startMs] に収まるように動画を加工する処理が必要
     *
     * @param videoPath 動画パス
     */
    @Serializable
    data class VideoItem(val videoPath: String) : TimelineItemType

}