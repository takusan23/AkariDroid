package io.github.takusan23.akaridroid.canvasrender

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.ImageRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.ItemRenderInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.TextRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.VideoRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** [Canvas] に [RenderData.CanvasItem]の内容を描画するやつ */
class CanvasRender(private val context: Context) {

    /** 描画する [ItemRenderInterface] の配列 */
    private var itemRenderList = emptyList<ItemRenderInterface>()

    /**
     * [RenderData]をセット、更新する
     *
     * @param canvasRenderItem 描画する
     */
    suspend fun setRenderData(canvasRenderItem: List<RenderData.CanvasItem>) = withContext(Dispatchers.IO) {
        itemRenderList = canvasRenderItem.map { renderItem ->
            // 描画するやつを用意する
            // 並列で初期化をする
            async {
                // データが変化していない場合は使い回す
                val existsOrNull = itemRenderList.firstOrNull { it.isEquals(renderItem) }
                if (existsOrNull != null) {
                    return@async existsOrNull
                }

                // 無ければ作る
                val newItem = when (renderItem) {
                    is RenderData.CanvasItem.Text -> TextRender(renderItem)
                    is RenderData.CanvasItem.Image -> ImageRender(context, renderItem)
                    is RenderData.CanvasItem.Video -> VideoRender(context, renderItem)
                }
                // 初期化も
                newItem.prepare()
                return@async newItem
            }
        }.awaitAll()
    }

    /**
     * 描画する
     *
     * @param canvas [Canvas]
     * @param durationMs 動画の合計時間
     * @param currentPositionMs 再生位置
     */
    suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.Default) {
        // 黒埋めする
        canvas.drawColor(Color.BLACK)
        // 描画すべきリスト
        val displayPositionItemList = itemRenderList.filter { it.isDisplayPosition(currentPositionMs) }
        // preDraw を並列で呼び出す
        displayPositionItemList.map { itemRender ->
            launch {
                itemRender.preDraw(
                    canvas = canvas,
                    durationMs = durationMs,
                    currentPositionMs = currentPositionMs
                )
            }
        }.joinAll()
        // 描画する
        // レイヤー順に
        displayPositionItemList
            .sortedBy { it.layerIndex }
            .forEach { itemRender ->
                itemRender.draw(
                    canvas = canvas,
                    durationMs = durationMs,
                    currentPositionMs = currentPositionMs
                )
            }
    }

    /** 破棄する */
    fun destroy() {
        itemRenderList.forEach {
            it.destroy()
        }
    }

}