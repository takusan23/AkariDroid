package io.github.takusan23.akaridroid.v2.canvasrender

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import io.github.takusan23.akaridroid.v2.canvasrender.itemrender.ImageRender
import io.github.takusan23.akaridroid.v2.canvasrender.itemrender.ItemRenderInterface
import io.github.takusan23.akaridroid.v2.canvasrender.itemrender.TextRender
import io.github.takusan23.akaridroid.v2.canvasrender.itemrender.VideoRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CanvasRender(private val context: Context) {

    /** 描画する [ItemRenderInterface] の配列 */
    private var itemRenderList = emptyList<ItemRenderInterface>()

    /**
     * [RenderData]をセット、更新する
     *
     * @param canvasRenderItem 描画する
     */
    suspend fun setRenderData(canvasRenderItem: List<RenderData.RenderItem>) = withContext(Dispatchers.IO) {
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
                    is RenderData.RenderItem.Text -> TextRender(renderItem)
                    is RenderData.RenderItem.Image -> ImageRender(context, renderItem)
                    is RenderData.RenderItem.Video -> VideoRender(renderItem)
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
     * @param currentPositionMs 再生位置
     */
    suspend fun draw(canvas: Canvas, currentPositionMs: Long) {
        itemRenderList
            .filter { currentPositionMs in it.displayTime }
            .forEach { itemRender -> itemRender.draw(canvas, currentPositionMs) }
    }

}