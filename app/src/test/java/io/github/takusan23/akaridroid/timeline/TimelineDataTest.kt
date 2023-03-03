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
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.TextItem("テスト 5秒", Color.RED, 50f),
                ),
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 5_000, endMs = 10_000,
                    timelineItemType = TimelineItemType.TextItem("テスト 10秒", Color.RED, 50f),
                ),
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.VideoItem("video.mp4"),
                ),
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 5_000, endMs = 10_000,
                    timelineItemType = TimelineItemType.VideoItem("video.mp4"),
                )
            )
        )
        val chunkList = timelineData.getTimelineChunkList()
        // それぞれ文字と図形が入っていること
        assertTrue { chunkList[0].timelineItemDataList.any { it.timelineItemType is TimelineItemType.TextItem } }
        assertTrue { chunkList[1].timelineItemDataList.any { it.timelineItemType is TimelineItemType.TextItem } }
        assertTrue { chunkList[0].timelineItemDataList.any { it.timelineItemType is TimelineItemType.VideoItem } }
        assertTrue { chunkList[1].timelineItemDataList.any { it.timelineItemType is TimelineItemType.VideoItem } }
    }

    @Test
    fun test_getTimelineChunkList_キャンバスだけのタイムライン() {
        val timelineData = TimelineData(
            videoDurationMs = 10_000,
            timelineItemDataList = listOf(
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.TextItem("テスト 5秒", Color.RED, 50f),
                ),
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 5_000, endMs = 10_000,
                    timelineItemType = TimelineItemType.TextItem("テスト 10秒", Color.RED, 50f),
                ),
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.RectItem(100f, 100f, Color.RED),
                ),
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 5_000, endMs = 10_000,
                    timelineItemType = TimelineItemType.RectItem(100f, 100f, Color.RED),
                )
            )
        )
        val chunkList = timelineData.getTimelineChunkList()
        // それぞれ文字と図形が入っていること
        assertEquals(chunkList[0].timelineItemDataList.count { it.timelineItemType is TimelineItemType.TextItem }, 2)
        assertEquals(chunkList[0].timelineItemDataList.count { it.timelineItemType is TimelineItemType.RectItem }, 2)
    }

    @Test
    fun test_getTimelineChunkList_動画とキャンバス混合のタイムライン() {
        val timelineData = TimelineData(
            videoDurationMs = 20_000,
            timelineItemDataList = listOf(
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 15_000,
                    timelineItemType = TimelineItemType.TextItem("エンコードテスト 15秒まで", Color.RED, 50f),
                ),
                TimelineItemData(
                    xPos = 0f, yPos = 0f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.VideoItem("toomo.mp4"),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 100f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.TextItem("トーモ バックレカラオケ", Color.RED, 80f),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 100f, startMs = 7_000, endMs = 9_000,
                    timelineItemType = TimelineItemType.TextItem("iPhone の動画（なし）", Color.RED, 80f),
                ),
                TimelineItemData(
                    xPos = 0f, yPos = 0f, startMs = 10_000, endMs = 15_000,
                    timelineItemType = TimelineItemType.VideoItem("cat.mp4"),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 100f, startMs = 10_000, endMs = 15_000,
                    timelineItemType = TimelineItemType.TextItem("ねこ", Color.RED, 80f),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 100f, startMs = 15_000, endMs = 20_000,
                    timelineItemType = TimelineItemType.TextItem("ねこ その2（なし）", Color.RED, 80f),
                ),
            )
        )
        val chunkList = timelineData.getTimelineChunkList()
        // エンコードテスト のテキストが複数のチャンクに入っていること
        assertEquals((chunkList[0].timelineItemDataList.first().timelineItemType as TimelineItemType.TextItem).text, "エンコードテスト 15秒まで")
        assertEquals((chunkList[1].timelineItemDataList.first().timelineItemType as TimelineItemType.TextItem).text, "エンコードテスト 15秒まで")
        assertEquals((chunkList[2].timelineItemDataList.first().timelineItemType as TimelineItemType.TextItem).text, "エンコードテスト 15秒まで")
        // 要素数があってること
        assertEquals(chunkList[0].timelineItemDataList.count(), 3)
        assertEquals(chunkList[1].timelineItemDataList.count(), 2)
        assertEquals(chunkList[2].timelineItemDataList.count(), 3)
        assertEquals(chunkList[3].timelineItemDataList.count(), 2)
        // 最初と最後の時間が合っていること
        assertEquals(chunkList.first().timelineItemDataList.first().startMs, 0)
        assertEquals(chunkList.last().timelineItemDataList.last().endMs, 20_000)
    }

}