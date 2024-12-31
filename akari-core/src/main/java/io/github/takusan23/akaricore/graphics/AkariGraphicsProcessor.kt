package io.github.takusan23.akaricore.graphics

import android.opengl.GLES20
import android.view.Surface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * OpenGL ES の上に構築された、映像フレームを作るやつ
 * SurfaceView を使う場合、[android.view.SurfaceHolder.setFixedSize]で[width]、[height]を入れておく必要があります。glViewport と違うとズレてしまうので。
 *
 * @param outputSurface 描画先。SurfaceView や MediaRecorder
 * @param width 映像の幅。フレームバッファーオブジェクトのために必要です
 * @param height 映像の高さ。フレームバッファーオブジェクトのために必要です
 * @param isEnableTenBitHdr 10-bit HDR を利用する場合は true。Camera2 API で 10-bit HDR の映像を扱う場合や、デコードする動画が 10-bit HDR の場合。
 *
 * TODO なんとかしてテストを書きたい
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class AkariGraphicsProcessor(
    outputSurface: Surface,
    private val width: Int,
    private val height: Int,
    isEnableTenBitHdr: Boolean
) {
    /** OpenGL 描画用スレッドの Kotlin Coroutine Dispatcher */
    private val openGlRelatedThreadDispatcher = newSingleThreadContext("openGlRelatedThreadDispatcher")

    private val inputSurface = AkariGraphicsInputSurface(outputSurface, isEnableTenBitHdr)
    private val textureRenderer = AkariGraphicsTextureRenderer(width, height, isEnableTenBitHdr)

    /** [AkariGraphicsTextureRenderer]等の用意をします */
    suspend fun prepare() {
        withContext(openGlRelatedThreadDispatcher) {
            inputSurface.makeCurrent()
            textureRenderer.prepareShader()
            GLES20.glViewport(0, 0, width, height)
        }
    }

    /** [AkariGraphicsSurfaceTexture]を作る際の引数 */
    suspend fun <T> genTextureId(action: (texId: Int) -> T): T = withContext(openGlRelatedThreadDispatcher) {
        textureRenderer.genTextureId(action)
    }

    /**
     * GL スレッドで呼び出される。
     * [AkariGraphicsEffectShader]を作る際などで使う。
     */
    suspend fun withOpenGlThread(action: suspend () -> Unit) {
        withContext(openGlRelatedThreadDispatcher) {
            action()
        }
    }

    /**
     * ループで描画する。
     * 連続してフレームを描画する場合は、[drawOneshot]をループで呼び出すよりこちらを使ってください。
     * [withContext]の呼び出しが地味に高コストなので。
     *
     * [LoopContinueData.isRequestNextFrame]が false か、コルーチンがキャンセルされたら終了します。
     *
     * @param draw このブロックは GL スレッドから呼び出されます
     * @return [LoopContinueData]。ループ継続かどうかと、MediaCodec / MediaRecorder に渡している場合は経過時間を渡してください。
     */
    suspend fun drawLoop(draw: suspend AkariGraphicsTextureRenderer.() -> LoopContinueData) {
        withContext(openGlRelatedThreadDispatcher) {
            while (true) {
                yield()
                textureRenderer.prepareDraw()
                val drawInfo = draw(textureRenderer)
                textureRenderer.drawEnd()
                // presentationTime、多分必要。
                // 無くても動く時があるが、AkariGraphicsProcessor が描画する場合は必要そう
                // 時間が当てにならなくなる
                inputSurface.setPresentationTime(drawInfo.currentFrameNanoSeconds)
                inputSurface.swapBuffers()
                if (!drawInfo.isRequestNextFrame) break
            }
        }
    }

    /**
     * 一回だけ描画する。
     * 一回限りのプレビュー更新など。
     *
     * @param draw このブロックは GL スレッドから呼び出されます
     */
    suspend fun drawOneshot(draw: suspend AkariGraphicsTextureRenderer.() -> Unit) {
        withContext(openGlRelatedThreadDispatcher) {
            textureRenderer.prepareDraw()
            draw(textureRenderer)
            textureRenderer.drawEnd()
            inputSurface.swapBuffers()
        }
    }

    /**
     * 破棄する
     * コルーチンキャンセル時に呼び出す場合、[kotlinx.coroutines.NonCancellable]をつけて呼び出す必要があります。
     *
     * @param preClean [AkariGraphicsTextureRenderer.destroy]よりも前に呼ばれる
     */
    suspend fun destroy(preClean: (suspend () -> Unit)? = null) {
        // 破棄自体も GL 用スレッドで呼び出す必要が多分ある
        withContext(openGlRelatedThreadDispatcher) {
            if (preClean != null) {
                preClean()
            }
            textureRenderer.destroy()
            inputSurface.destroy()
        }
        // もう使わない
        openGlRelatedThreadDispatcher.close()
    }


    /**
     * [drawLoop]でループを続行するかと、現在の動画時間。
     *
     * [currentFrameNanoSeconds]について
     * [AkariGraphicsProcessor]の[inputSurface]として[android.media.MediaCodec]/[android.media.MediaRecorder]を使うと、出力された動画の時間が増えすぎてしまう。
     * 動画フレームの時間を提供するためにこの値を使う。
     * 録画じゃなくてプレビューのみの場合は[currentFrameNanoSeconds]は 0 固定で良いはず。
     * また、[android.media.MediaRecorder]の場合は常に[System.nanoTime]の値を渡す必要がありそうです。（要調査）
     *
     * @param isRequestNextFrame 次フレームの作成をするか。しない場合は[drawLoop]を終わります。
     * @param currentFrameNanoSeconds ループを開始してからの経過時間（ナノ秒）。ミリ秒から変換する場合は、[LoopContinueData.MILLI_SECONDS_TO_NANO_SECONDS] で掛け算する。
     */
    data class LoopContinueData(
        var isRequestNextFrame: Boolean,
        var currentFrameNanoSeconds: Long
    ) {
        companion object {
            /** ミリ秒からナノ秒に変換する際の掛け算のかけられる数 */
            const val MILLI_SECONDS_TO_NANO_SECONDS = 1_000_000
        }
    }
}