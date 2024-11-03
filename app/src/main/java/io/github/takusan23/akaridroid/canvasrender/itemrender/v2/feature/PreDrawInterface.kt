package io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature

/** [DrawCanvasInterface.draw]や[DrawSurfaceTextureInterface.draw]よりも前に処理をしたい場合 */
interface PreDrawInterface {

    /**
     * 動画のフレームの取得など、時間がかかる場合は[preDraw]を利用してください。
     * [DrawCanvasInterface.draw]や[DrawSurfaceTextureInterface.draw]の前に呼ばれて、並列で[preDraw]します。すべての呼び出しが完了した後に draw されます。
     * 重い処理をしない場合は、このメソッドを実装する必要はありません。
     */
    suspend fun preDraw(durationMs: Long, currentPositionMs: Long)

}