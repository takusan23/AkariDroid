package io.github.takusan23.akaridroid.canvasrender

import android.content.Context
import android.view.Surface
import android.view.SurfaceHolder
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer
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
    private val surfaceVariantFlow = MutableStateFlow<SurfaceVariant?>(null)

    /** パラメーターあとから変更したいので Flow */
    private val videoParametersFlow = MutableStateFlow<VideoParameters?>(null)

    /** [surfaceVariantFlow]と[videoParametersFlow]から[AkariGraphicsProcessor]を作る */
    private val akariGraphicsProcessorFlow = combine(
        surfaceVariantFlow,
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

            val surfaceVariant = next.first
            val videoParameters = next.second
            if (surfaceVariant != null && videoParameters != null) {
                val (outputWidth, outputHeight, isEnableTenBitHdr) = videoParameters

                // SurfaceHolder の場合は AkariGraphicsProcessor の glViewport に合わせる
                if (surfaceVariant is SurfaceVariant.SurfaceHolder) {
                    surfaceVariant.holder.setFixedSize(outputWidth, outputHeight)
                }

                // OpenGL ES の上に構築された動画フレーム描画システム
                // prepare() 後に emit() する
                val newAkariGraphicsProcessor = AkariGraphicsProcessor(
                    outputSurface = surfaceVariant.surface,
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

    // TODO 違いを書く
    fun setOutputSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        surfaceVariantFlow.value = surfaceHolder?.let { SurfaceVariant.SurfaceHolder(it) }
    }

    // TODO 違いを書く
    fun setOutputSurface(surface: Surface?) {
        surfaceVariantFlow.value = surface?.let { SurfaceVariant.Surface(it) }
    }

    // TODO 違いを書く
    suspend fun setRenderData(canvasRenderItem: List<RenderData.CanvasItem>) {
        val akariGraphicsProcessor = akariGraphicsProcessorFlow.filterNotNull().first()
        setRenderData(canvasRenderItem, akariGraphicsProcessor)
    }

    // TODO 違いを書く
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

        // 描画すべき動画素材を取得
        val displayPositionItemList = prepareDraw(akariGraphicsProcessor, durationMs, currentPositionMs)

        // 描画する
        // レイヤー順に
        // TODO エンコードには使わないのであんまりないはずだが、eglPresentationTimeANDROID を呼び出せるようにする
        akariGraphicsProcessor.drawOneshot {
            drawItemRendererToAkariGraphicsProcessor(
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                videoParameters = videoParameters,
                akariGraphicsTextureRenderer = this,
                displayPositionItemList = displayPositionItemList.sortedBy { it.layerIndex }
            )
        }
    }

    /**
     * エンコード（動画の書き出し）時用。
     * 動画の時間になるまでループする。多分こっちのほうが withContext 呼び出しが減るので良いはず。
     *
     * @param durationMs 動画の時間
     * @param frameRate
     */
    suspend fun drawLoop(
        durationMs: Long,
        frameRate: Int,
        onProgress: (currentPositionMs: Long) -> Unit
    ) {
        // AkariGraphicsProcessor が生成されるまで待つ
        val akariGraphicsProcessor = akariGraphicsProcessorFlow.filterNotNull().first()
        val videoParameters = videoParametersFlow.filterNotNull().first()

        // 1フレームの時間
        // 60fps なら 16ms、30fps なら 33ms
        val frameMs = 1_000 / frameRate
        // 作成済み動画の時間
        var currentPositionMs = 0L

        val loopContinueData = AkariGraphicsProcessor.LoopContinueData(true, 0)
        akariGraphicsProcessor.drawLoop {
            onProgress(currentPositionMs)

            // 今の時間と続行するかを入れる
            loopContinueData.currentFrameMs = currentPositionMs
            loopContinueData.isRequestNextFrame = currentPositionMs <= durationMs

            // 描画する
            drawItemRendererToAkariGraphicsProcessor(
                durationMs = durationMs,
                currentPositionMs = currentPositionMs,
                videoParameters = videoParameters,
                akariGraphicsTextureRenderer = this,
                // TODO これシングルスレッドでいいかな
                displayPositionItemList = prepareDraw(
                    akariGraphicsProcessor = akariGraphicsProcessor,
                    durationMs = durationMs,
                    currentPositionMs = currentPositionMs
                )
            )

            // 時間を増やす
            // 1 フレーム分の時間。ミリ秒なので増やす
            currentPositionMs += frameMs
            // ループ情報を返す
            loopContinueData
        }
    }

    /** 破棄する */
    fun destroy() {
        scope.cancel()
    }

    private suspend fun drawItemRendererToAkariGraphicsProcessor(
        durationMs: Long,
        currentPositionMs: Long,
        videoParameters: VideoParameters,
        akariGraphicsTextureRenderer: AkariGraphicsTextureRenderer,
        displayPositionItemList: List<RendererInterface>
    ) {
        displayPositionItemList.forEach { itemRender ->
            when {
                itemRender is DrawCanvasInterface -> {
                    akariGraphicsTextureRenderer.drawCanvas {
                        itemRender.draw(this, durationMs, currentPositionMs)
                    }
                }

                itemRender is DrawSurfaceTextureInterface -> {
                    akariGraphicsTextureRenderer.drawSurfaceTexture(
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
                        akariGraphicsTextureRenderer.applyEffect(itemRender.akariGraphicsEffectShader!!)
                    }
                }
            }
        }
    }

    /**
     * [draw]の準備を行う。
     * タイムラインの動画素材の準備（やもう使われなくなった素材の破棄）や、[PreDrawInterface.preDraw]の呼び出しをする。
     *
     * @param akariGraphicsProcessor [AkariGraphicsProcessor]
     * @param durationMs 動画時間
     * @param currentPositionMs 動画の再生位置
     * @return 描画すべき動画時間。[RendererInterface]
     */
    private suspend fun prepareDraw(
        akariGraphicsProcessor: AkariGraphicsProcessor,
        durationMs: Long,
        currentPositionMs: Long
    ): List<RendererInterface> {

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
            .ifEmpty { null } ?: return emptyList()

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

        return displayPositionItemList
    }

    private data class VideoParameters(
        val outputWidth: Int,
        val outputHeight: Int,
        val isEnableTenBitHdr: Boolean = false
    )

    /**
     * Surface / SurfaceHolder どっちも受け付けできるように。
     * [SurfaceVariant.SurfaceHolder]の場合は、[android.view.SurfaceHolder.setFixedSize]を自動で呼び出します。
     */
    private interface SurfaceVariant {
        val surface: android.view.Surface

        data class Surface(override val surface: android.view.Surface) : SurfaceVariant

        data class SurfaceHolder(val holder: android.view.SurfaceHolder) : SurfaceVariant {
            override val surface: android.view.Surface = holder.surface
        }
    }

}