package io.github.takusan23.akaridroid

import android.graphics.Color
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.processor.*
import io.github.takusan23.akaricore.v1.tool.MediaExtractorTool
import io.github.takusan23.akaricore.v1.processor.AudioMixingProcessor
import io.github.takusan23.akaricore.v1.processor.CanvasProcessor
import io.github.takusan23.akaricore.v1.processor.ConcatProcessor
import io.github.takusan23.akaricore.v1.processor.CutProcessor
import io.github.takusan23.akaricore.v1.processor.VideoCanvasProcessor
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
                TimelineItemData.CanvasData(
                    xPos = 0f, yPos = 0f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.RectItem(1280f, 720f, Color.WHITE),
                ),
                // テキスト
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 100f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("前半戦", Color.CYAN, 80f),
                ),
                // 背景
                TimelineItemData.CanvasData(
                    xPos = 0f, yPos = 0f, startMs = 5_000, endMs = 10_000,
                    timelineDrawItemType = TimelineDrawItemType.RectItem(1280f, 720f, Color.CYAN),
                ),
                // テキスト
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 100f, startMs = 5_000, endMs = 10_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("後半戦", Color.WHITE, 80f),
                ),
                // テキスト
                TimelineItemData.CanvasData(
                    xPos = 500f, yPos = 500f, startMs = 0, endMs = 10_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("エンコードテスト", Color.RED, 20f),
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
        val canvasDraw = TimelineCanvasDraw(timelineData.timelineItemDataList.filterIsInstance<TimelineItemData.CanvasData>())
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
                TimelineItemData.VideoData(
                    startMs = 0, endMs = 5_000, videoCutStartMs = 0, videoCutEndMs = 5_000,
                    videoFilePath = sampleVideoFolder.resolve("toomo.mp4").path,
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 100f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("とーも", Color.WHITE, 80f),
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 300f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("ばっくれからおけ", Color.WHITE, 80f),
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
            val timelineCanvasDraw = TimelineCanvasDraw(chunkData.timelineItemDataList.filterIsInstance<TimelineItemData.CanvasData>())
            // 動画がある、探す
            val originVideoData = chunkData.timelineItemDataList.filterIsInstance<TimelineItemData.VideoData>().first()
            // 動画を範囲内にカットする
            val videoFile = originVideoData.videoCutRange?.let { cutRange ->
                tempFolder.resolve("cutFile").also { result ->
                    // TODO カット範囲を入力可能にする
                    CutProcessor.cut(File(originVideoData.videoFilePath), result, cutRange, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)
                }
            } ?: File(originVideoData.videoFilePath)

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
                TimelineItemData.VideoData(
                    startMs = 0, endMs = 5_000, videoCutStartMs = 0, videoCutEndMs = 5_000,
                    videoFilePath = sampleVideoFolder.resolve("toomo.mp4").path,
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 100f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("とーも", Color.WHITE, 80f),
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 300f, startMs = 0, endMs = 5_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("ばっくれからおけ", Color.WHITE, 80f),
                ),
                TimelineItemData.CanvasData(
                    xPos = 0f, yPos = 0f, startMs = 5_000, endMs = 10_000,
                    timelineDrawItemType = TimelineDrawItemType.RectItem(1280f, 720f, Color.BLACK),
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 100f, startMs = 5_000, endMs = 10_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("ダーク尺余り", Color.WHITE, 80f),
                ),
                TimelineItemData.CanvasData(
                    xPos = 100f, yPos = 500f, startMs = 0, endMs = 10_000,
                    timelineDrawItemType = TimelineDrawItemType.TextItem("エンコードのテスト中", Color.RED, 80f),
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
            val timelineCanvasDraw = TimelineCanvasDraw(chunkData.timelineItemDataList.filterIsInstance<TimelineItemData.CanvasData>())
            val videoFileOrNull = chunkData.timelineItemDataList.filterIsInstance<TimelineItemData.VideoData>().firstOrNull()
            if (videoFileOrNull != null) {
                // 動画がある
                val videoFile = videoFileOrNull.videoCutRange?.let { cutRange ->
                    // 動画を範囲内にカットする必要があればする
                    tempFolder.resolve("cutFile").also { resultFile ->
                        CutProcessor.cut(File(videoFileOrNull.videoFilePath), resultFile, cutRange, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)
                    }
                } ?: File(videoFileOrNull.videoFilePath)
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

    @Test
    fun test_タイムラインから音声のミキシングが出来る() = runTest(dispatchTimeoutMs = DEFAULT_DISPATCH_TIMEOUT_MS * 10) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleVideoFolder = File(appContext.getExternalFilesDir(null), "sample")
        val resultFile = File(appContext.getExternalFilesDir(null), "test_タイムラインから音声のミキシングが出来る_${System.currentTimeMillis()}.aac").apply { createNewFile() }
        val tempFolder = File(appContext.getExternalFilesDir(null), "temp").apply {
            deleteRecursively()
            mkdir()
        }

        val timelineData = TimelineData(
            videoDurationMs = 10_000,
            timelineItemDataList = listOf(
                TimelineItemData.AudioData(
                    startMs = 0, endMs = 10_000,
                    audioFilePath = sampleVideoFolder.resolve("famipop.mp3").path,
                ),
                TimelineItemData.VideoData(
                    startMs = 0, endMs = 10_000,
                    videoFilePath = sampleVideoFolder.resolve("iphone.mp4").path,
                ),
                TimelineItemData.AudioData(
                    startMs = 0, endMs = 2_000,
                    audioFilePath = sampleVideoFolder.resolve("yukkuri.mp3").path,
                ),
                TimelineItemData.AudioData(
                    startMs = 5_000, endMs = 7_000,
                    audioFilePath = sampleVideoFolder.resolve("yukkuri.mp3").path,
                ),
            )
        )
        // 音声と動画のみ
        val mixingList = timelineData.timelineItemDataList.filterIsInstance<TimelineItemData.VideoData>().map { itemData ->
            AudioMixingProcessor.MixingFileData(File(itemData.videoFilePath), itemData.timeRange, volume = 1f)
        } + timelineData.timelineItemDataList.filterIsInstance<TimelineItemData.AudioData>().map { itemData ->
            AudioMixingProcessor.MixingFileData(File(itemData.audioFilePath), itemData.timeRange, volume = 0.05f)
        }
        AudioMixingProcessor.start(mixingList, resultFile, tempFolder, timelineData.videoDurationMs)
        tempFolder.deleteRecursively()
    }

    companion object {
        /** runTest デフォルトタイムアウト */
        private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L
    }

}