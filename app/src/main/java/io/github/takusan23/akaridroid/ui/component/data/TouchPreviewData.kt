package io.github.takusan23.akaridroid.ui.component.data

import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.data.TouchPreviewData.TouchPreviewItem

/**
 * [io.github.takusan23.akaridroid.ui.component.TouchPreviewCanvas]で渡すデータ
 *
 * @param videoSize 動画の縦横サイズ
 * @param visibleCanvasItemList 表示すべきキャンバス要素。シーク位置に連動させるのは ViewModel とかで。[TouchPreviewItem]の配列
 */
data class TouchPreviewData(
    val videoSize: RenderData.Size,
    val visibleCanvasItemList: List<TouchPreviewItem>
) {

    /**
     * キャンバス要素がとこにあるか
     *
     * @param id [RenderData.CanvasItem.id]と同じ
     * @param size サイズ
     * @param position 位置。[RenderData.CanvasItem.position]
     */
    data class TouchPreviewItem(
        val id: Long,
        val size: RenderData.Size,
        val position: RenderData.Position
    )

    /**
     * ドラッグアンドドロップでキャンバス要素を移動させたときに渡されるデータ
     *
     * @param id [RenderData.CanvasItem.id]とおなじ
     * @param size サイズ
     * @param position 移動先の位置
     */
    data class PositionUpdateRequest(
        val id: Long,
        val size: RenderData.Size,
        val position: RenderData.Position
    )

}