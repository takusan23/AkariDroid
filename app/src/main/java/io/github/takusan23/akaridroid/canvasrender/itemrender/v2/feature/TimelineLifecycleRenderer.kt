package io.github.takusan23.akaridroid.canvasrender.itemrender.v2.feature

/**
 * タイムラインで必要な時にだけリソースを確保する。
 * ハードウェアデコーダー等は数に限りがあるため、今の再生位置に必要な動画素材だけデコーダーを起動するため、などに使われる。
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