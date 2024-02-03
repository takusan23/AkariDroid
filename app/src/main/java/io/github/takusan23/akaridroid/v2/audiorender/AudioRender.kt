package io.github.takusan23.akaridroid.v2.audiorender

import android.content.Context
import io.github.takusan23.akaridroid.v2.canvasrender.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRender(private val context: Context) {

    /**
     * [RenderData]をセット、更新する
     *
     * @param canvasRenderItem 描画する
     */
    suspend fun setRenderData(canvasRenderItem: List<RenderData.RenderItem>) = withContext(Dispatchers.IO) {

    }

    suspend fun getPcmByteArray() {

    }

}