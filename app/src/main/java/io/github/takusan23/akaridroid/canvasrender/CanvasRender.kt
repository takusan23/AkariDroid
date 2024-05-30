package io.github.takusan23.akaridroid.canvasrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.BaseItemRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.EffectRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.ImageRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.ShaderRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.ShapeRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.SwitchAnimationRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.TextRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.VideoRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** [Canvas] に [RenderData.CanvasItem]の内容を描画するやつ */
class CanvasRender(private val context: Context) {

    /** 描画する [BaseItemRender] の配列 */
    private var itemRenderList = emptyList<BaseItemRender>()

    /** [Bitmap] 転写で使う [Paint] */
    private val paint = Paint()

    /**
     * [RenderData]をセット、更新する
     *
     * @param canvasRenderItem 描画する
     */
    suspend fun setRenderData(canvasRenderItem: List<RenderData.CanvasItem>) = withContext(Dispatchers.IO) {

        // 前の呼び出しから消えた素材はリソース開放させる
        itemRenderList.forEach { renderItem ->
            if (canvasRenderItem.none { renderItem.isEquals(it) }) {
                renderItem.setLifecycle(BaseItemRender.RenderLifecycleState.DESTROYED)
            }
        }

        itemRenderList = canvasRenderItem.map { renderItem ->
            // データが変化していない場合は使い回す
            itemRenderList.firstOrNull {
                it.isEquals(renderItem)
            } ?: when (renderItem) { // 無ければ作る
                is RenderData.CanvasItem.Text -> TextRender(context, renderItem)
                is RenderData.CanvasItem.Image -> ImageRender(context, renderItem)
                is RenderData.CanvasItem.Video -> VideoRender(context, renderItem)
                is RenderData.CanvasItem.Shape -> ShapeRender(renderItem)
                is RenderData.CanvasItem.Shader -> ShaderRender(renderItem)
                is RenderData.CanvasItem.SwitchAnimation -> SwitchAnimationRender(renderItem)
                is RenderData.CanvasItem.Effect -> EffectRender(renderItem)
            }
        }
    }

    /**
     * 描画する
     *
     * @param outCanvas [Canvas]
     * @param durationMs 動画の合計時間
     * @param currentPositionMs 再生位置
     */
    suspend fun draw(outCanvas: Canvas, durationMs: Long, currentPositionMs: Long) = withContext(Dispatchers.Default) {

        // 黒埋めする
        outCanvas.drawColor(Color.BLACK)

        // Canvas に直接書くのではなく、一旦別の Canvas に書いて、全部描き終わったら Bitmap を引数の outCanvas へ転写する
        // これをすることで、描いている途中のフレームにエフェクトをかけられるようになる。
        val tempBitmap = Bitmap.createBitmap(outCanvas.width, outCanvas.height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)

        // 時間的にもう使われない場合は DESTROY にする
        itemRenderList
            .filter { itemRender -> itemRender.currentLifecycleState == BaseItemRender.RenderLifecycleState.PREPARED }
            .filter { itemRender -> !itemRender.isDisplayPosition(currentPositionMs) }
            .map { itemRender -> launch { itemRender.setLifecycle(BaseItemRender.RenderLifecycleState.DESTROYED) } }
            .joinAll()

        // 描画すべきリスト
        val displayPositionItemList = itemRenderList.filter { it.isDisplayPosition(currentPositionMs) }

        // 描画すべきリストで PREPARED していない場合は呼び出す
        // 必要な素材のみ準備する
        displayPositionItemList
            .filter { it.currentLifecycleState == BaseItemRender.RenderLifecycleState.DESTROYED }
            .map { itemRender -> launch { itemRender.setLifecycle(BaseItemRender.RenderLifecycleState.PREPARED) } }
            .joinAll()

        // preDraw を並列で呼び出す
        displayPositionItemList.map { itemRender ->
            launch {
                itemRender.preDraw(
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
                    canvas = tempCanvas,
                    drawFrame = tempBitmap,
                    durationMs = durationMs,
                    currentPositionMs = currentPositionMs
                )
            }

        // outCanvas へ転写する
        outCanvas.drawBitmap(tempBitmap, 0f, 0f, paint)
    }

    /** 破棄する */
    suspend fun destroy() {
        itemRenderList.forEach { itemRender ->
            itemRender.setLifecycle(BaseItemRender.RenderLifecycleState.DESTROYED)
        }
    }

}