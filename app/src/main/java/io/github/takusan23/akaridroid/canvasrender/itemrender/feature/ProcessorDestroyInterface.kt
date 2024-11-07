package io.github.takusan23.akaridroid.canvasrender.itemrender.feature

/** [io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor]が破棄された時に破棄する必要があれば */
interface ProcessorDestroyInterface {

    /**
     * [io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor.destroy]の preClean で呼ばれます。
     * [io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture.detachGl]等を呼び出すために使います。
     */
    suspend fun destroyProcessorGl()

}