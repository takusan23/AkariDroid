package io.github.takusan23.akaridroid.canvasrender

import android.content.Context
import android.view.SurfaceHolder
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.BaseItemRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.DrawCanvas
import io.github.takusan23.akaridroid.canvasrender.itemrender.DrawSurfaceTexture
import io.github.takusan23.akaridroid.canvasrender.itemrender.EffectRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.ImageRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.ShaderRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.ShapeRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.SwitchAnimationRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.TextRender
import io.github.takusan23.akaridroid.canvasrender.itemrender.VideoRender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

/**
 * タイムラインの素材を映像に出力する
 * [RenderData.CanvasItem]の素材を使って、[AkariGraphicsProcessor]でフレームを作り、SurfaceView / MediaCodec に出力する
 */
class VideoRenderer(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    /** SurfaceView の Surface はコールバックで受け取る必要があり、コンストラクタでは受け取れない。Flow でいい感じに受け取る。null で破棄判定 */
    private val surfaceHolderFlow = MutableStateFlow<SurfaceHolder?>(null)

    /** パラメーターあとから変更したいので Flow */
    private val videoParametersFlow = MutableStateFlow<VideoParameters?>(null)

    /** [surfaceHolderFlow]と[videoParametersFlow]から[AkariGraphicsProcessor]を作る */
    private val akariGraphicsProcessorFlow = combine(
        surfaceHolderFlow,
        videoParametersFlow,
        ::Pair
    ).let { combineFlow ->
        var currentAkariGraphicsProcessor: AkariGraphicsProcessor? = null
        combineFlow.transform { next ->

            println("next = $next")

            // 今の AkariGraphicsProcessor は破棄する
            // もう使えないため emit() で null
            // TODO コンテキストを切り離す detach 手段を用意する
            emit(null)
            currentAkariGraphicsProcessor?.destroy {
                itemRenderList.forEach {
                    if (it is DrawSurfaceTexture) {
                        it.akariGraphicsSurfaceTexture.detachGl()
                    }
                }
            }
            currentAkariGraphicsProcessor = null

            val surfaceHolder = next.first
            val videoParameters = next.second
            if (surfaceHolder != null && videoParameters != null) {
                val (outputWidth, outputHeight, isEnableTenBitHdr) = videoParameters

                // AkariGraphicsProcessor の glViewport に合わせる
                surfaceHolder.setFixedSize(outputWidth, outputHeight)

                // OpenGL ES の上に構築された動画フレーム描画システム
                // prepare() 後に emit() する
                val newAkariGraphicsProcessor = AkariGraphicsProcessor(
                    outputSurface = surfaceHolder.surface,
                    width = outputWidth,
                    height = outputHeight,
                    isEnableTenBitHdr = isEnableTenBitHdr
                )
                newAkariGraphicsProcessor.prepare()
                emit(newAkariGraphicsProcessor)
                currentAkariGraphicsProcessor = newAkariGraphicsProcessor
            }
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    /** 描画する [BaseItemRender] の配列 */
    private var itemRenderList = emptyList<BaseItemRender>()

    fun setVideoParameters(
        outputWidth: Int,
        outputHeight: Int,
        isEnableTenBitHdr: Boolean = false
    ) {
        videoParametersFlow.value = VideoParameters(outputWidth, outputHeight, isEnableTenBitHdr)
    }

    fun setOutputSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        surfaceHolderFlow.value = surfaceHolder
    }

    suspend fun setRenderData(canvasRenderItem: List<RenderData.CanvasItem>) {
        // AkariGraphicsProcessor が生成されるまで待つ
        val akariGraphicsProcessor = akariGraphicsProcessorFlow.filterNotNull().first()

        // 前の呼び出しから消えた素材はリソース開放させる
        itemRenderList.forEach { renderItem ->
            if (canvasRenderItem.none { renderItem.isEquals(it) }) {
                renderItem.setLifecycle(BaseItemRender.RenderLifecycleState.DESTROYED)
            }
        }

        itemRenderList = canvasRenderItem.map { renderItem ->
            // データが変化していない場合は使い回す
            itemRenderList.firstOrNull { it.isEquals(renderItem) } ?: when (renderItem) { // 無ければ作る
                is RenderData.CanvasItem.Text -> TextRender(context, renderItem)
                is RenderData.CanvasItem.Image -> ImageRender(context, renderItem)
                is RenderData.CanvasItem.Video -> akariGraphicsProcessor.genTextureId { texId -> VideoRender(context, renderItem, texId) }
                is RenderData.CanvasItem.Shape -> ShapeRender(renderItem)
                is RenderData.CanvasItem.Shader -> ShaderRender(renderItem)
                is RenderData.CanvasItem.SwitchAnimation -> SwitchAnimationRender(renderItem)
                is RenderData.CanvasItem.Effect -> EffectRender(renderItem)
            }
        }
    }

    suspend fun draw(durationMs: Long, currentPositionMs: Long) {
        // AkariGraphicsProcessor が生成されるまで待つ
        val akariGraphicsProcessor = akariGraphicsProcessorFlow.filterNotNull().first()
        val videoParameters = videoParametersFlow.filterNotNull().first()

        // 時間的にもう使われない場合は DESTROY にする
        coroutineScope {
            itemRenderList
                .filter { itemRender -> itemRender.currentLifecycleState == BaseItemRender.RenderLifecycleState.PREPARED }
                .filter { itemRender -> !itemRender.isDisplayPosition(currentPositionMs) }
                .map { itemRender -> launch { itemRender.setLifecycle(BaseItemRender.RenderLifecycleState.DESTROYED) } }
        }

        // 描画すべきリスト
        val displayPositionItemList = itemRenderList.filter { it.isDisplayPosition(currentPositionMs) }

        // 描画すべきリストで PREPARED していない場合は呼び出す
        // 必要な素材のみ準備する
        coroutineScope {
            displayPositionItemList
                .filter { it.currentLifecycleState == BaseItemRender.RenderLifecycleState.DESTROYED }
                .map { itemRender -> launch { itemRender.setLifecycle(BaseItemRender.RenderLifecycleState.PREPARED) } }
        }

        // preDraw を並列で呼び出す
        coroutineScope {
            displayPositionItemList.map { itemRender ->
                launch {
                    itemRender.preDraw(
                        durationMs = durationMs,
                        currentPositionMs = currentPositionMs
                    )
                }
            }
        }

        // 描画する
        // レイヤー順に
        val layerSortedRenderItemList = displayPositionItemList.sortedBy { it.layerIndex }
        // TODO akariGraphicsProcessor.drawOneshot { } で何も描画しないと落ちるので対策
        if (layerSortedRenderItemList.isEmpty()) return

        // TODO eglPresentationTimeANDROID を呼び出せるようにする
        akariGraphicsProcessor.drawOneshot {

            layerSortedRenderItemList.forEach { itemRender ->
                // TODO エフェクトが無い。作り直す
                when (itemRender) {
                    is DrawCanvas -> {
                        drawCanvas {
                            itemRender.draw(this, durationMs, currentPositionMs)
                        }
                    }

                    is DrawSurfaceTexture -> {
                        drawSurfaceTexture(
                            akariSurfaceTexture = itemRender.akariGraphicsSurfaceTexture,
                            isAwaitTextureUpdate = false,
                            onTransform = { mvpMatrix -> itemRender.draw(mvpMatrix, videoParameters.outputWidth, videoParameters.outputHeight) }
                        )
                    }
                }
            }
        }
    }

    /** 破棄する */
    suspend fun destroy() {
        itemRenderList.forEach { itemRender ->
            itemRender.setLifecycle(BaseItemRender.RenderLifecycleState.DESTROYED)
        }
        scope.cancel()
    }

    private data class VideoParameters(
        val outputWidth: Int,
        val outputHeight: Int,
        val isEnableTenBitHdr: Boolean = false
    )

}