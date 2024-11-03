package io.github.takusan23.akaridroid.canvasrender

import android.content.Context
import android.view.SurfaceHolder
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.EffectRenderer
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.ImageRenderer
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.ShaderRenderer
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.ShapeRenderer
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.SwitchAnimationRenderer
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.TextRenderer
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.VideoRenderer
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.DrawCanvasInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.DrawFragmentShaderInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.DrawSurfaceTextureInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.GlTimelineLifecycleInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.PreDrawInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.ProcessorDestroyInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.RendererInterface
import io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature.TimelineLifecycleRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

/**
 * タイムラインの素材を描画して、動画フレームとして出力する。
 * [RenderData.CanvasItem]の素材を使って、[AkariGraphicsProcessor]でフレームを作り、SurfaceView / MediaCodec に出力する。
 *
 * TODO なんとかしてテストを書きたい
 */
class VideoTrackRenderer(private val context: Context) {
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

            // 今の AkariGraphicsProcessor は破棄する
            // もう使えないため emit() で null
            emit(null)
            currentAkariGraphicsProcessor?.destroy {
                itemRenderList
                    .filterIsInstance<ProcessorDestroyInterface>()
                    .forEach { renderer -> renderer.destroyProcessorGl() }
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

    /** 描画する [RendererInterface] の配列。基底インターフェース */
    private var itemRenderList = emptyList<RendererInterface>()

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
        val akariGraphicsProcessor = akariGraphicsProcessorFlow.filterNotNull().first()
        setRenderData(canvasRenderItem, akariGraphicsProcessor)
    }

    suspend fun suspendObserveAkariGraphicsProcessorReCreate(canvasRenderItem: List<RenderData.CanvasItem>) {
        // SurfaceView の Surface 再生成などで AkariGraphicsProcessor 自体が再生成される
        // 再生成されると、OpenGL ES コンテキストに依存している AkariGraphicsEffectShader 等は使えなくなってしまう。 TODO コンテキスト間共有できないか調べる
        // そのため、再生成されたら作り直す必要がある。
        // setRenderData でセットしたので初回は skip
        akariGraphicsProcessorFlow.drop(1).collectLatest { akariGraphicsProcessor ->
            akariGraphicsProcessor ?: return@collectLatest
            setRenderData(canvasRenderItem, akariGraphicsProcessor)
        }
    }

    private suspend fun setRenderData(canvasRenderItem: List<RenderData.CanvasItem>, akariGraphicsProcessor: AkariGraphicsProcessor) {

        // 前の呼び出しから消えた素材はリソース開放させる
        itemRenderList.forEach { renderItem ->
            if (canvasRenderItem.none { renderItem.isEquals(it) }) {
                when (renderItem) {
                    is TimelineLifecycleRenderer -> renderItem.leaveTimeline()
                    is GlTimelineLifecycleInterface -> akariGraphicsProcessor.withOpenGlThread { renderItem.leaveTimelineGl() }
                }
            }
        }

        itemRenderList = canvasRenderItem.map { renderItem ->
            // データが変化していない場合は使い回す
            itemRenderList.firstOrNull { it.isEquals(renderItem) } ?: when (renderItem) { // 無ければ作る
                is RenderData.CanvasItem.Effect -> EffectRenderer(renderItem)
                is RenderData.CanvasItem.Image -> ImageRenderer(context, renderItem)
                is RenderData.CanvasItem.Shader -> ShaderRenderer(renderItem)
                is RenderData.CanvasItem.Shape -> ShapeRenderer(renderItem)
                is RenderData.CanvasItem.SwitchAnimation -> SwitchAnimationRenderer(renderItem)
                is RenderData.CanvasItem.Text -> TextRenderer(context, renderItem)
                is RenderData.CanvasItem.Video -> akariGraphicsProcessor.genTextureId { texId -> VideoRenderer(context, renderItem, texId) }
            }
        }
    }

    suspend fun draw(durationMs: Long, currentPositionMs: Long) {
        // AkariGraphicsProcessor が生成されるまで待つ
        val akariGraphicsProcessor = akariGraphicsProcessorFlow.filterNotNull().first()
        val videoParameters = videoParametersFlow.filterNotNull().first()

        // タイムラインの時間外になったら破棄する
        coroutineScope {
            itemRenderList
                .filter { renderer -> renderer.isEnterTimeline && !renderer.isDisplayPosition(currentPositionMs) }
                .map { renderer ->
                    launch {
                        when (renderer) {
                            is TimelineLifecycleRenderer -> renderer.leaveTimeline()
                            is GlTimelineLifecycleInterface -> akariGraphicsProcessor.withOpenGlThread { renderer.leaveTimelineGl() }
                        }
                    }
                }
        }

        // 描画すべきリスト
        val displayPositionItemList = itemRenderList
            .filter { it.isDisplayPosition(currentPositionMs) }
            .ifEmpty { null } ?: return

        // タイムラインに入ったことを伝えてない場合は伝える
        // 必要な素材のみ準備する
        coroutineScope {
            displayPositionItemList
                .filter { renderer -> !renderer.isEnterTimeline }
                .map { renderer ->
                    launch {
                        when (renderer) {
                            is TimelineLifecycleRenderer -> renderer.enterTimeline()
                            is GlTimelineLifecycleInterface -> akariGraphicsProcessor.withOpenGlThread { renderer.enterTimelineGl() }
                        }
                    }
                }
        }

        // preDraw が必要な場合は
        coroutineScope {
            displayPositionItemList
                .filterIsInstance<PreDrawInterface>()
                .map { itemRender ->
                    launch {
                        itemRender.preDraw(
                            durationMs = durationMs,
                            currentPositionMs = currentPositionMs
                        )
                    }
                }
        }

        // TODO eglPresentationTimeANDROID を呼び出せるようにする
        akariGraphicsProcessor.drawOneshot {

            // 描画する
            // レイヤー順に
            displayPositionItemList
                .sortedBy { it.layerIndex }
                .forEach { itemRender ->
                    when {
                        itemRender is DrawCanvasInterface -> {
                            drawCanvas {
                                itemRender.draw(this, durationMs, currentPositionMs)
                            }
                        }

                        itemRender is DrawSurfaceTextureInterface -> {
                            drawSurfaceTexture(
                                akariSurfaceTexture = itemRender.akariGraphicsSurfaceTexture,
                                isAwaitTextureUpdate = false,
                                onTransform = { mvpMatrix -> itemRender.draw(mvpMatrix, videoParameters.outputWidth, videoParameters.outputHeight) }
                            )
                        }

                        itemRender is DrawFragmentShaderInterface -> {
                            if (itemRender.akariGraphicsEffectShader != null) {
                                itemRender.preEffect(
                                    width = videoParameters.outputWidth,
                                    height = videoParameters.outputHeight,
                                    durationMs = durationMs,
                                    currentPositionMs = currentPositionMs
                                )
                                applyEffect(itemRender.akariGraphicsEffectShader!!)
                            }
                        }
                    }
                }
        }
    }

    /** 破棄する */
    fun destroy() {
        scope.cancel()
    }

    private data class VideoParameters(
        val outputWidth: Int,
        val outputHeight: Int,
        val isEnableTenBitHdr: Boolean = false
    )

}