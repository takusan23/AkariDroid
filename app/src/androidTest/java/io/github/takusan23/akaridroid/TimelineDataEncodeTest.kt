package io.github.takusan23.akaridroid

import android.graphics.Color
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.processor.CanvasProcessor
import io.github.takusan23.akaricore.processor.ConcatProcessor
import io.github.takusan23.akaricore.processor.CutProcessor
import io.github.takusan23.akaricore.processor.VideoCanvasProcessor
import io.github.takusan23.akaricore.tool.MediaExtractorTool
import io.github.takusan23.akaridroid.data.VideoOutputFormat
import io.github.takusan23.akaridroid.timeline.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineDataEncodeTest {

    @Test
    fun test_キャンバスのみのタイムラインから動画を作成できる() = runTest(dispatchTimeoutMs = DEFAULT_DISPATCH_TIMEOUT_MS * 10) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val resultFile = File(appContext.getExternalFilesDir(null), "result_${System.currentTimeMillis()}.mp4").apply {
            createNewFile()
        }

        val timelineData = TimelineData(
            videoDurationMs = 10_000,
            timelineItemDataList = listOf(
                // 背景
                TimelineItemData(
                    xPos = 0f, yPos = 0f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.RectItem(1280f, 720f, Color.WHITE),
                ),
                // テキスト
                TimelineItemData(
                    xPos = 100f, yPos = 100f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.TextItem("前半戦", Color.CYAN, 80f),
                ),
                // 背景
                TimelineItemData(
                    xPos = 0f, yPos = 0f, startMs = 5_000, endMs = 10_000,
                    timelineItemType = TimelineItemType.RectItem(1280f, 720f, Color.CYAN),
                ),
                // テキスト
                TimelineItemData(
                    xPos = 100f, yPos = 100f, startMs = 5_000, endMs = 10_000,
                    timelineItemType = TimelineItemType.TextItem("後半戦", Color.WHITE, 80f),
                ),
                // テキスト
                TimelineItemData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 10_000,
                    timelineItemType = TimelineItemType.TextItem("エンコードテスト", Color.RED, 20f),
                )
            )
        )

        val outputFormat = VideoOutputFormat(
            videoCodec = VideoOutputFormat.VideoCodec.AVC,
            videoWidth = 1280,
            videoHeight = 720,
            frameRate = 30,
            bitRate = 5_000_000
        )

        // タイムラインのデータをもとに Canvas に描画する
        val canvasDraw = TimelineCanvasDraw(timelineData.timelineItemDataList)
        CanvasProcessor.start(
            resultFile = resultFile,
            videoCodec = MediaFormat.MIMETYPE_VIDEO_AVC,
            containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            bitRate = outputFormat.bitRate,
            frameRate = outputFormat.frameRate,
            outputVideoWidth = outputFormat.videoWidth,
            outputVideoHeight = outputFormat.videoHeight,
        ) { positionMs ->
            // 描画する
            canvasDraw.draw(this, positionMs)
            // true を返している間は動画を作成する
            positionMs < timelineData.videoDurationMs
        }
    }

    @Test
    fun test_動画のみのタイムラインから動画を作成できる() = runTest(dispatchTimeoutMs = DEFAULT_DISPATCH_TIMEOUT_MS * 10) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleVideoFolder = File(appContext.getExternalFilesDir(null), "sample")
        val tempFolder = File(appContext.getExternalFilesDir(null), "temp").apply { mkdir() }
        val resultFile = File(appContext.getExternalFilesDir(null), "test_動画のみのタイムラインから動画を作成できる_${System.currentTimeMillis()}.mp4").apply { createNewFile() }

        val timelineData = TimelineData(
            videoDurationMs = 5_000,
            timelineItemDataList = listOf(
                TimelineItemData(
                    xPos = 0f, yPos = 0f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.VideoItem(sampleVideoFolder.resolve("toomo.mp4").path),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 100f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.TextItem("とーも", Color.WHITE, 80f),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 300f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.TextItem("ばっくれからおけ", Color.WHITE, 80f),
                ),
            )
        )
        // エンコードする
        val outputFormat = VideoOutputFormat(
            videoCodec = VideoOutputFormat.VideoCodec.AVC,
            videoWidth = 1280,
            videoHeight = 720,
            frameRate = 30,
            bitRate = 5_000_000
        )
        // 各チャンクごとにエンコードする
        val chunkList = timelineData.getTimelineChunkList()
        val encodeVideoChunkFileList = chunkList.mapIndexed { index, chunkData ->
            val encodedFile = tempFolder.resolve("video_$index.mp4").apply { createNewFile() }
            val timelineCanvasDraw = TimelineCanvasDraw(chunkData.timelineItemDataList)
            // 動画がある、探す
            val originVideoFile = File((chunkData.timelineItemDataList.first { it.timelineItemType is TimelineItemType.VideoItem }.timelineItemType as TimelineItemType.VideoItem).videoPath)
            // 動画を範囲内にカットする
            val videoFile = tempFolder.resolve("cutFile")

            // TODO カット範囲を入力可能にする
            CutProcessor.cut(originVideoFile, videoFile, 0..chunkData.durationMs, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)
            // えんこーど
            VideoCanvasProcessor.start(
                videoFile = videoFile,
                resultFile = encodedFile,
                videoCodec = MediaFormat.MIMETYPE_VIDEO_AVC,
                containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                bitRate = outputFormat.bitRate,
                frameRate = outputFormat.frameRate,
                outputVideoWidth = outputFormat.videoWidth,
                outputVideoHeight = outputFormat.videoHeight
            ) { positionMs -> timelineCanvasDraw.draw(this, positionMs) }
            encodedFile
        }
        ConcatProcessor.concatVideo(encodeVideoChunkFileList, resultFile)
        tempFolder.deleteRecursively()
    }

    @Test
    fun test_動画とキャンバスのタイムラインから動画を作成できる() = runTest(dispatchTimeoutMs = DEFAULT_DISPATCH_TIMEOUT_MS * 10) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleVideoFolder = File(appContext.getExternalFilesDir(null), "sample")
        val resultFile = File(appContext.getExternalFilesDir(null), "test_動画とキャンバスのタイムラインから動画を作成できる_${System.currentTimeMillis()}.mp4").apply { createNewFile() }
        val tempFolder = File(appContext.getExternalFilesDir(null), "temp").apply {
            deleteRecursively()
            mkdir()
        }

        val timelineData = TimelineData(
            videoDurationMs = 10_000,
            timelineItemDataList = listOf(
                TimelineItemData(
                    xPos = 0f, yPos = 0f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.VideoItem(sampleVideoFolder.resolve("toomo.mp4").path),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 100f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.TextItem("とーも", Color.WHITE, 80f),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 300f, startMs = 0, endMs = 5_000,
                    timelineItemType = TimelineItemType.TextItem("ばっくれからおけ", Color.WHITE, 80f),
                ),
                TimelineItemData(
                    xPos = 0f, yPos = 0f, startMs = 5_000, endMs = 10_000,
                    timelineItemType = TimelineItemType.RectItem(1280f, 720f, Color.BLACK),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 100f, startMs = 5_000, endMs = 10_000,
                    timelineItemType = TimelineItemType.TextItem("ダーク尺余り", Color.WHITE, 80f),
                ),
                TimelineItemData(
                    xPos = 100f, yPos = 500f, startMs = 0, endMs = 10_000,
                    timelineItemType = TimelineItemType.TextItem("エンコードのテスト中", Color.RED, 80f),
                ),
            )
        )
        // エンコードする
        val outputFormat = VideoOutputFormat(
            videoCodec = VideoOutputFormat.VideoCodec.AVC,
            videoWidth = 1280,
            videoHeight = 720,
            frameRate = 30,
            bitRate = 5_000_000
        )
        // 各チャンクごとにエンコードする
        val encodeVideoChunkFileList = timelineData.getTimelineChunkList().mapIndexed { index, chunkData ->
            val encodedFile = tempFolder.resolve("video_$index.mp4").apply { createNewFile() }
            val timelineCanvasDraw = TimelineCanvasDraw(chunkData.timelineItemDataList)
            val requireVideoCanvasProcessor = chunkData.timelineItemDataList.any { it.timelineItemType is TimelineItemType.VideoItem }
            if (requireVideoCanvasProcessor) {
                // 動画がある、探す
                val originVideoFile = File((chunkData.timelineItemDataList.first { it.timelineItemType is TimelineItemType.VideoItem }.timelineItemType as TimelineItemType.VideoItem).videoPath)
                // 動画を範囲内にカットする
                val videoFile = tempFolder.resolve("cutFile")
                // TODO カット範囲を入力可能にする
                CutProcessor.cut(originVideoFile, videoFile, 0..chunkData.durationMs, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)
                // えんこーど
                VideoCanvasProcessor.start(
                    videoFile = videoFile,
                    resultFile = encodedFile,
                    videoCodec = MediaFormat.MIMETYPE_VIDEO_AVC,
                    containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                    bitRate = outputFormat.bitRate,
                    frameRate = outputFormat.frameRate,
                    outputVideoWidth = outputFormat.videoWidth,
                    outputVideoHeight = outputFormat.videoHeight
                ) { positionMs -> timelineCanvasDraw.draw(this, chunkData.startMs + positionMs) }
            } else {
                // 動画ない
                CanvasProcessor.start(
                    resultFile = encodedFile,
                    videoCodec = MediaFormat.MIMETYPE_VIDEO_AVC,
                    containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                    bitRate = outputFormat.bitRate,
                    frameRate = outputFormat.frameRate,
                    outputVideoWidth = outputFormat.videoWidth,
                    outputVideoHeight = outputFormat.videoHeight
                ) { positionMs ->
                    // TODO オフセットの計算が面倒すぎる
                    val includePositionMs = chunkData.startMs + positionMs
                    timelineCanvasDraw.draw(this, includePositionMs)
                    // true を返している間は動画を作成する
                    includePositionMs <= chunkData.endMs
                }
            }
            return@mapIndexed encodedFile
        }
        ConcatProcessor.concatVideo(encodeVideoChunkFileList, resultFile)
        tempFolder.deleteRecursively()
    }

    companion object {
        /** runTest デフォルトタイムアウト */
        private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L
    }

}