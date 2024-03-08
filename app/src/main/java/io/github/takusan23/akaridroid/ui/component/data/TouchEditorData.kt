package io.github.takusan23.akaridroid.ui.component.data

import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.data.TouchEditorData.TouchEditorItem

/**
 * [io.github.takusan23.akaridroid.ui.component.VideoPlayerPreviewAndTouchEditor]で渡すデータ
 *
 * @param videoSize 動画の縦横サイズ
 * @param visibleTouchEditorItemList 表示すべきキャンバス要素。シーク位置に連動させるのは ViewModel とかで。[TouchEditorItem]の配列
 */
data class TouchEditorData(
    val videoSize: RenderData.Size,
    val visibleTouchEditorItemList: List<TouchEditorItem>
) {

    /**
     * キャンバス要素がとこにあるか
     *
     * @param id [RenderData.CanvasItem.id]と同じ
     * @param size サイズ
     * @param position 位置。[RenderData.CanvasItem.position]
     */
    data class TouchEditorItem(
        val id: Long,
        val size: RenderData.Size,
        val position: RenderData.Position
    )

    /**
     * ドラッグアンドドロップでキャンバス要素を移動させたときに渡されるデータ
     *
     * @param id [RenderData.CanvasItem.id]とおなじ
     * @param position 移動先の位置
     */
    data class PositionUpdateRequest(
        val id: Long,
        val position: RenderData.Position
    )

    /**
     * ピンチイン、ピンチアウトでキャンバス要素の大きさが変化した時に渡されるデータ
     *
     * @param id [RenderData.CanvasItem.id]とおなじ
     * @param size 縦横サイズ
     */
    data class SizeChangeRequest(
        val id: Long,
        val size: RenderData.Size
    )

}