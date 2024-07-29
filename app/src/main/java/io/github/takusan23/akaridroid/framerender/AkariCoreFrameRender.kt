package io.github.takusan23.akaridroid.framerender

import android.content.Context
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [AkariGraphicsProcessor]へ動画フレームを描画する。
 * 描画すると[AkariGraphicsProcessor]で指定した Surface （SurfaceView / MediaCodec）に動画フレームが送信されます。
 *
 * [AkariGraphicsProcessor]は OpenGL ES の上に構築された動画フレーム描画システムなので、ちょっとややこしい。
 */
class AkariCoreFrameRender(
    private val context: Context,
    private val genTextureId: suspend () -> Int
) {

    /** 描画する [BaseItemRender] の配列 */
    private var itemRenderList = emptyList<BaseItemRender>()

    /**
     * [RenderData]をセット、更新する。
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
            // 無ければ作る
            itemRenderList.firstOrNull { it.isEquals(renderItem) } ?: renderItem.createRender()
        }
    }

    /**
     * 描画する。
     * GL スレッドから呼び出されます。
     *
     * @param durationMs 動画の合計時間
     * @param currentPositionMs 再生位置
     */
    suspend fun draw(
        textureRenderer: AkariGraphicsTextureRenderer,
        durationMs: Long,
        currentPositionMs: Long
    ) = coroutineScope { // 親のスコープを引き継ぐため

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
        // TODO OpenGL ES で書き直したら多分 preDraw いらんかも
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
                    textureRenderer = textureRenderer,
                    durationMs = durationMs,
                    currentPositionMs = currentPositionMs
                )
            }
    }

    /** 破棄する */
    suspend fun destroy() {
        itemRenderList.forEach { itemRender ->
            itemRender.setLifecycle(BaseItemRender.RenderLifecycleState.DESTROYED)
        }
    }

    /** [RenderData.CanvasItem]から対応した ItemRender を作る */
    private suspend fun RenderData.CanvasItem.createRender(): BaseItemRender = when (this) {
        is RenderData.CanvasItem.Text -> TextRender(context, this)
        is RenderData.CanvasItem.Image -> ImageRender(context, this)
        is RenderData.CanvasItem.Video -> VideoRender(genTextureId(), context, this)
        is RenderData.CanvasItem.Shape -> ShapeRender(this)
        is RenderData.CanvasItem.Shader -> ShaderRender(this)
        is RenderData.CanvasItem.SwitchAnimation -> SwitchAnimationRender(this)
        is RenderData.CanvasItem.Effect -> EffectRender(this)
    }

}