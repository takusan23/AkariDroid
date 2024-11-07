package io.github.takusan23.akaridroid.canvasrender.itemrender.feature

/** [TimelineLifecycleRenderer]と同じですが、GL スレッドで呼び出されるのが約束されます。 */
abstract class GlTimelineLifecycleInterface : RendererInterface {

    final override var isEnterTimeline: Boolean = false
        protected set

    /** タイムラインの時間に入った時に一回呼ばれる。 */
    open suspend fun enterTimelineGl() {
        isEnterTimeline = true
    }

    /** タイムラインの時間外になったとき、もしくは破棄時に呼び出される。 */
    open suspend fun leaveTimelineGl() {
        isEnterTimeline = false
    }

}