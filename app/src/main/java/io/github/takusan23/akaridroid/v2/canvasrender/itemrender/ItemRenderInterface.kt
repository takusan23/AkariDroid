package io.github.takusan23.akaridroid.v2.canvasrender.itemrender

import android.graphics.Canvas
import io.github.takusan23.akaridroid.v2.canvasrender.RenderData

/** [io.github.takusan23.akaridroid.v2.canvasrender.RenderData.RenderItem]を描画する */
interface ItemRenderInterface {

    /** いつ描画すべきか */
    val displayTime: RenderData.DisplayTime

    /**
     * 用意の際に呼ばれる
     * 別スレッドで呼ばれます
     */
    suspend fun prepare()

    /**
     * 渡された[Canvas]に描画する
     *
     * @param canvas 描画先
     * @param currentPositionMs 描画したい時間
     */
    suspend fun draw(canvas: Canvas, currentPositionMs: Long)

    /**
     * 破棄する
     * [prepare]で用意したリソースを開放してください
     */
    suspend fun destroy()

    /**
     * データが一緒かどうか返す
     * [io.github.takusan23.akaridroid.v2.canvasrender.RenderData.CanvasItem]を比較して、変化していれば更新してね
     */
    suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean

}