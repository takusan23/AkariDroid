package io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature

import io.github.takusan23.akaridroid.RenderData

/**
 * [io.github.takusan23.akaridroid.canvasrender.itemrender.v2.TextRenderer]等の基底インターフェース。
 * 詳しくは[TimelineLifecycleRenderer]で。
 */
sealed interface RendererInterface {

    /** タイムラインの再生位置に含まれていれば true */
    val isEnterTimeline: Boolean

    /** レイヤー。タイムラインのレーン番号です */
    val layerIndex: Int

    /**
     * データが一緒かどうか返す
     *
     * @param renderItem 比較対象の[RenderData.CanvasItem]
     * @return 同じ場合は true
     */
    suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean

    /**
     * 描画すべき時間を渡すので、描画すべきかどうかを返す
     * もしここで false を返した場合、[preDraw]、[draw]は呼ばれません
     *
     * @param currentPositionMs 描画する時間
     * @return true の場合描画する
     */
    suspend fun isDisplayPosition(currentPositionMs: Long): Boolean

}