package io.github.takusan23.akaridroid.v2.render

/** [RenderData.DisplayTime]を [LongRange] に */
val RenderData.DisplayTime.range: LongRange
    get() = startMs..stopMs

/** [RenderData.canvasRenderItem] が指定時間内にあるものを取り出す */
fun RenderData.filterTimeContains(timeMs: Long): List<RenderData.RenderItem> = canvasRenderItem.filter { timeMs in it.displayTime.range }