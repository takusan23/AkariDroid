package io.github.takusan23.akaridroid.v2.render

import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CanvasRenderer {

    /** [renderData]を[Canvas]に描画する */
    suspend fun canvasAkariRenderer(
        canvas: Canvas,
        currentMs: Long,
        renderData: RenderData
    ): Unit = withContext(Dispatchers.Default) {
        canvas.apply {
            val paint = Paint()
            renderData.filterTimeContains(currentMs).forEach { renderItem ->
                when (renderItem) {
                    is RenderData.RenderItem.Text -> {
                        paint.color = renderItem.fontColor
                        paint.textSize = renderItem.textSize
                        drawText(renderItem.text, renderItem.position.x.toFloat(), renderItem.position.y.toFloat(), paint)
                    }

                    is RenderData.RenderItem.Image -> {
                        // なんかキャッシュするやつを作る
                        TODO()
                    }

                    is RenderData.RenderItem.Video -> {
                        // なんかデコーダーを一回だけ作るやつを作る
                        TODO()
                    }
                }
            }
        }
    }
}