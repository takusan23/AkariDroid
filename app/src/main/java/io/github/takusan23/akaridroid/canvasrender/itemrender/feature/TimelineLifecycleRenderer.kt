package io.github.takusan23.akaridroid.canvasrender.itemrender.feature

/**
 * タイムラインの動画素材を描画する。
 * 画像や文字、動画の描画はこのクラスを継承した Renderer によって行われます。
 *
 * # ライフサイクル
 * 例えば動画のハードウェアデコーダーは数に限りがあるので、
 * タイムライン上にある動画全てでハードウェアデコーダーを起動しようとすると数によっては足りない。
 * ので、必要になるまで初期化用関数を呼び出さない。
 *
 * - タイムラインで必要になった
 *  - [enterTimeline]
 * - 描画
 *  - [PreDrawInterface.preDraw] -> [DrawCanvasInterface.draw] [DrawSurfaceTextureInterface.draw] [DrawFragmentShaderInterface.preEffect]
 * - タイムラインで不要になった。もしくは破棄が必要な場合。
 *  - [leaveTimeline]
 *
 * [io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor]の GL スレッドから呼び出される[GlTimelineLifecycleInterface]もあります。
 */
abstract class TimelineLifecycleRenderer : RendererInterface {

    final override var isEnterTimeline = false
        private set

    /** タイムラインの時間に入った時に一回呼ばれる。 */
    open suspend fun enterTimeline() {
        isEnterTimeline = true
    }

    /** タイムラインの時間外になったとき、もしくは破棄時に呼び出される。 */
    open suspend fun leaveTimeline() {
        isEnterTimeline = false
    }
}