package io.github.takusan23.akaridroid.v2.canvasrender.itemrender

import android.graphics.Canvas
import io.github.takusan23.akaridroid.v2.RenderData

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
     * 動画のフレーム（指定位置の動画の Bitmap）の取得など、時間がかかる場合は[preDraw]を利用してください。
     * [draw]の前に呼ばれて、並列で[preDraw]します。すべての呼び出しが完了した後に[draw]されます。
     * [draw]で重い処理をしない場合は、このメソッドを実装する必要はありません。
     */
    suspend fun preDraw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) {
        // 実装は任意です
    }

    /**
     * 渡された[Canvas]に描画する
     *
     * @param canvas 描画先
     * @param durationMs 動画の合計時間
     * @param currentPositionMs 描画したい時間
     */
    suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long)

    /**
     * 破棄する
     * [prepare]で用意したリソースを開放してください
     */
    fun destroy()

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