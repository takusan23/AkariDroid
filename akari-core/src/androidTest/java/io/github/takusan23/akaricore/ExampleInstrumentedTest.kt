package io.github.takusan23.akaricore

import android.graphics.Color
import android.graphics.Paint
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.processor.*
import io.github.takusan23.akaricore.tool.MediaExtractorTool
import io.github.takusan23.akaricore.tool.MediaMuxerTool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /**
     * 音声合成ができるのかテスト
     *
     * 保存先は /storage/emulated/0/Android/data/io.github.takusan23.akari_core.test/files
     */
    @Test
    fun test_音声を合成できる() = runTest(dispatchTimeoutMs = DEFAULT_DISPATCH_TIMEOUT_MS * 10) {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val tempFolder = File(appContext.getExternalFilesDir(null), "temp").apply {
            deleteRecursively()
            mkdir()
        }
        val resultFile = File(appContext.getExternalFilesDir(null), "result_${System.currentTimeMillis()}").apply {
            delete()
            createNewFile()
        }
        val videoEditFolder = File(appContext.getExternalFilesDir(null), "project")
        val projectFolder = File(videoEditFolder, "project-2022-01-10")

        val videoFile = File(projectFolder, "videofile")
        val bgmFile = File(projectFolder, "bgm")

        val mixingProcessor = AudioMixingProcessor(
            audioFileList = listOf(videoFile, bgmFile),
            resultFile = resultFile,
            tempWorkFolder = tempFolder,
            audioCodec = MediaFormat.MIMETYPE_AUDIO_AAC,
            mixingVolume = 0.05f
        )
        mixingProcessor.start()

        tempFolder.deleteRecursively()

        assertTrue(resultFile.length() > 0)
    }

    @Test
    fun test_アスペクト比を考慮してエンコードできる() = runTest(dispatchTimeoutMs = DEFAULT_DISPATCH_TIMEOUT_MS * 10) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val resultFile = File(appContext.getExternalFilesDir(null), "result_${System.currentTimeMillis()}.mp4").apply {
            delete()
            createNewFile()
        }
        val videoEditFolder = File(appContext.getExternalFilesDir(null), "project")
        val projectFolder = File(videoEditFolder, "project-2022-01-10")

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 80f
        }

        // val videoFile = File(projectFolder, "applehls.mp4")
        // val videoFile = File(projectFolder, "apple_4x3_10s.mp4")
        val videoFile = File(projectFolder, "iphone.mp4")
        val videoCanvasProcessor = VideoCanvasProcessor(
            videoFile = videoFile,
            resultFile = resultFile,
            videoCodec = MediaFormat.MIMETYPE_VIDEO_AVC,
            containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            outputVideoWidth = 1280,
            outputVideoHeight = 720
        )
        videoCanvasProcessor.start { positionMs ->
            drawText("再生時間 = ${"%.02f".format((positionMs / 1000F))} 秒", 50f, 80f, paint)
        }
    }

    @Test
    fun test_キャンバスの入力から動画を作成() = runTest(dispatchTimeoutMs = DEFAULT_DISPATCH_TIMEOUT_MS * 10) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val resultFile = File(appContext.getExternalFilesDir(null), "result_${System.currentTimeMillis()}.mp4").apply { createNewFile() }

        val canvasProcessor = CanvasProcessor(
            resultFile = resultFile,
            videoCodec = MediaFormat.MIMETYPE_VIDEO_AVC,
            containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            bitRate = 1_000_000,
            frameRate = 30,
            outputVideoWidth = 1280,
            outputVideoHeight = 720,
        )

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 80f
        }

        canvasProcessor.start { positionMs ->
            drawColor(Color.MAGENTA)
            // 枠取り文字
            val text = "動画の時間 = ${"%.2f".format(positionMs / 1000f)}"
            textPaint.color = Color.BLACK
            textPaint.style = Paint.Style.STROKE
            // 枠取り文字
            drawText(text, 0f, 80f, textPaint)
            textPaint.style = Paint.Style.FILL
            textPaint.color = Color.WHITE
            // 枠無し文字
            drawText(text, 0f, 80f, textPaint)

            // true を返している間は動画を作成する
            positionMs < 10 * 1_000
        }
    }

    @Test
    fun test_動画の結合ができる() = runTest(dispatchTimeoutMs = DEFAULT_DISPATCH_TIMEOUT_MS * 10) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleVideoFolder = appContext.getExternalFilesDir(null)!!.resolve("sample")
        val resultFile = File(appContext.getExternalFilesDir(null), "result_${System.currentTimeMillis()}.mp4").apply { createNewFile() }
        val tempFolder = appContext.getExternalFilesDir(null)!!.resolve("temp").apply {
            deleteRecursively()
            mkdir()
        }
        // 予め用意している動画
        val iphone = sampleVideoFolder.resolve("iphone.mp4")
        val cat = sampleVideoFolder.resolve("cat.mp4")
        val toomo = sampleVideoFolder.resolve("toomo.mp4")
        // 音声と映像を一時的なファイルに保存
        val concatMediaList = listOf(
            ConcatProcessor.concatVideo(listOf(iphone, cat, toomo), tempFolder.resolve("concat_video")),
            ConcatProcessor.concatAudio(listOf(iphone, cat, toomo), tempFolder, tempFolder.resolve("concat_audio"))
        )
        // 結合する
        MediaMuxerTool.mixed(resultFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4, concatMediaList)
        // あとしまつ
        tempFolder.deleteRecursively()
    }

    @Test
    fun test_動画の切り取りが出来る() = runTest(dispatchTimeoutMs = DEFAULT_DISPATCH_TIMEOUT_MS * 10) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleVideoFolder = appContext.getExternalFilesDir(null)!!.resolve("sample")
        val resultFile = File(appContext.getExternalFilesDir(null), "test_動画の切り取りが出来る_${System.currentTimeMillis()}.mp4").apply { createNewFile() }
        val toomo = sampleVideoFolder.resolve("toomo.mp4")
        // カットしてみる
        CutProcessor.cut(toomo, resultFile, 0L..2_000L, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)
    }

    companion object {
        /** runTest デフォルトタイムアウト */
        private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L
    }
}