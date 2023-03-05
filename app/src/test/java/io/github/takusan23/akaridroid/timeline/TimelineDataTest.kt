package io.github.takusan23.akaridroid.timeline

import android.graphics.Color
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [TimelineData.getTimelineChunkList]のテスト */
class TimelineDataTest {

    @Test
    fun test_getTimelineChunkList_動画だけのタイムライン() {
        val timelineData = TimelineData(
            videoDurationMs = 10_000,
            timelineItemDataList = listOf(
                TimelineItemData.CanvasData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("テスト 5秒", Color.RED, 50f),
                ),
                TimelineItemData.CanvasData(
                    xPos = 500f, yPos = 500f, startMs = 5_000, endMs = 10_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("テスト 10秒", Color.RED, 50f),
                ),
                TimelineItemData.VideoData(
                    startMs = 0, endMs = 5_000,
                    videoFilePath = "video.mp4",
                ),
                TimelineItemData.VideoData(
                    startMs = 5_000, endMs = 10_000,
                    videoFilePath = "video.mp4",
                )
            )
        )
        val chunkList = timelineData.getTimelineChunkList()
        // それぞれ文字と図形が入っていること
        assertTrue { chunkList[0].timelineItemDataList.any { it is TimelineItemData.CanvasData } }
        assertTrue { chunkList[1].timelineItemDataList.any { it is TimelineItemData.CanvasData } }
        assertTrue { chunkList[0].timelineItemDataList.any { it is TimelineItemData.VideoData } }
        assertTrue { chunkList[1].timelineItemDataList.any { it is TimelineItemData.VideoData } }
        // startMs / endMs があっていること
        assertEquals(chunkList[0].startMs, 0)
        assertEquals(chunkList[0].endMs, 5_000)
    }

    @Test
    fun test_getTimelineChunkList_キャンバスだけのタイムライン() {
        val timelineData = TimelineData(
            videoDurationMs = 10_000,
            timelineItemDataList = listOf(
                TimelineItemData.CanvasData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("テスト 5秒", Color.RED, 50f),
                ),
                TimelineItemData.CanvasData(
                    xPos = 500f, yPos = 500f, startMs = 5_000, endMs = 10_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("テスト 10秒", Color.RED, 50f),
                ),
                TimelineItemData.CanvasData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.RectItem(100f, 100f, Color.RED),
                ),
                TimelineItemData.CanvasData(
                    xPos = 500f, yPos = 500f, startMs = 5_000, endMs = 10_000,
                    timelineDrawItemType = TimelineDrawItemType.RectItem(100f, 100f, Color.RED),
                )
            )
        )
        val chunkList = timelineData.getTimelineChunkList()
        // それぞれ文字と図形が入っていること
        assertEquals(chunkList[0].timelineItemDataList.filterIsInstance<TimelineItemData.CanvasData>().count { it.timelineDrawItemType is TimelineDrawItemType.TextItem }, 2)
        assertEquals(chunkList[0].timelineItemDataList.filterIsInstance<TimelineItemData.CanvasData>().count { it.timelineDrawItemType is TimelineDrawItemType.RectItem }, 2)
        // startMs / endMs があっていること
        assertEquals(chunkList[0].startMs, 0)
        assertEquals(chunkList[0].endMs, 10_000)
    }

    @Test
    fun test_getTimelineChunkList_動画とキャンバス混合のタイムライン() {
        val timelineData = TimelineData(
            videoDurationMs = 20_000,
            timelineItemDataList = listOf(
                TimelineItemData.CanvasData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 15_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("エンコードテスト 15秒まで", Color.RED, 50f),
                ),
                TimelineItemData.VideoData(
                    startMs = 0, endMs = 5_000,
                    videoFilePath = "toomo.mp4",
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 100f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("トーモ バックレカラオケ", Color.RED, 80f),
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 100f, startMs = 7_000, endMs = 9_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("iPhone の動画（なし）", Color.RED, 80f),
                ),
                TimelineItemData.VideoData(
                    startMs = 10_000, endMs = 15_000,
                    videoFilePath = "cat.mp4",
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 100f, startMs = 10_000, endMs = 15_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("ねこ", Color.RED, 80f),
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 100f, startMs = 15_000, endMs = 20_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("ねこ その2（なし）", Color.RED, 80f),
                ),
            )
        )
        val chunkList = timelineData.getTimelineChunkList()
        // エンコードテスト のテキストが複数のチャンクに入っていること
        assertEquals((chunkList[0].timelineItemDataList.filterIsInstance<TimelineItemData.CanvasData>().first().timelineDrawItemType as TimelineDrawItemType.TextItem).text, "エンコードテスト 15秒まで")
        assertEquals((chunkList[1].timelineItemDataList.filterIsInstance<TimelineItemData.CanvasData>().first().timelineDrawItemType as TimelineDrawItemType.TextItem).text, "エンコードテスト 15秒まで")
        assertEquals((chunkList[2].timelineItemDataList.filterIsInstance<TimelineItemData.CanvasData>().first().timelineDrawItemType as TimelineDrawItemType.TextItem).text, "エンコードテスト 15秒まで")
        // 要素数があってること
        assertEquals(chunkList[0].timelineItemDataList.count(), 3)
        assertEquals(chunkList[1].timelineItemDataList.count(), 2)
        assertEquals(chunkList[2].timelineItemDataList.count(), 3)
        assertEquals(chunkList[3].timelineItemDataList.count(), 2)
        // startMs / endMs があっていること
        assertEquals(chunkList[0].startMs, 0)
        assertEquals(chunkList[0].endMs, 5_000)
        assertEquals(chunkList[1].startMs, 5_000)
        assertEquals(chunkList[1].endMs, 10_000)
        // 最初と最後の時間が合っていること
        assertEquals(chunkList.first().timelineItemDataList.first().startMs, 0)
        assertEquals(chunkList.last().timelineItemDataList.last().endMs, 20_000)
    }

}