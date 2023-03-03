package io.github.takusan23.akaridroid.timeline

import android.graphics.Canvas
import android.graphics.Paint

/**
 * タイムラインに描画するやつ
 *
 * @param timelineData タイムラインのデータ
 */
class TimelineCanvasDraw(private val timelineItemList: List<TimelineItemData>) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    /**
     * 描画する
     *
     * @param canvas 描画先 Canvas
     * @param positionMs 再生時間（ミリ秒）
     */
    fun draw(canvas: Canvas, positionMs: Long) {
        val drawList = timelineItemList.filter { positionMs in it.timeRange }
        drawList.forEach { item ->
            when (val type = item.timelineItemType) {
                is TimelineItemType.TextItem -> {
                    paint.color = type.color
                    paint.textSize = type.fontSize
                    canvas.drawText(type.text, item.xPos, item.yPos, paint)
                }
                is TimelineItemType.RectItem -> {
                    paint.color = type.color
                    canvas.drawRect(item.xPos, item.yPos, type.width, type.height, paint)
                }
                is TimelineItemType.VideoItem -> {
                    // 動画の描画は Canvas ではしない
                }
            }
        }
    }

}