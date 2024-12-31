package io.github.takusan23.akaridroid.canvasrender.itemrender.feature

import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.VideoTrackRendererPrepareData

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
     * 使い回しできるかを返す
     * 動画素材を描画する際に使う、動画デコーダー等は破棄せず使いまわしたほうが良い気がして。
     *
     * @param renderItem 比較対象の[RenderData.CanvasItem]
     * @param videoTrackRendererPrepareData 映像トラックの情報。例えば 10-bit HDR の有無によって作り直す必要があればこれを見て false を返してください。特に関係なく動く場合は[renderItem]だけの比較で良いです。
     * @return 同じ場合は true
     */
    suspend fun isReuse(renderItem: RenderData.CanvasItem, videoTrackRendererPrepareData: VideoTrackRendererPrepareData): Boolean

    /**
     * 描画すべき時間を渡すので、描画すべきかどうかを返す
     * もしここで false を返した場合、[preDraw]、[draw]は呼ばれません
     *
     * @param currentPositionMs 描画する時間
     * @return true の場合描画する
     */
    suspend fun isDisplayPosition(currentPositionMs: Long): Boolean

}