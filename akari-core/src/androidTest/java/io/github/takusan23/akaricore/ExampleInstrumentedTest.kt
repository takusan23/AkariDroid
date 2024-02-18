package io.github.takusan23.akaricore

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.v2.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.v2.audio.AudioMixingProcessor
import io.github.takusan23.akaricore.v2.audio.AudioVolumeProcessor
import io.github.takusan23.akaricore.v2.audio.ReSamplingRateProcessor
import io.github.takusan23.akaricore.v2.audio.SilenceAudioProcessor
import io.github.takusan23.akaricore.v2.common.CutProcessor
import io.github.takusan23.akaricore.v2.common.MediaExtractorTool
import io.github.takusan23.akaricore.v2.common.toAkariCoreInputDataSource
import io.github.takusan23.akaricore.v2.video.CanvasVideoProcessor
import io.github.takusan23.akaricore.v2.video.VideoFrameBitmapExtractor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    // 各テストの保存先は /storage/emulated/0/Android/data/io.github.takusan23.akari_core.test/files

    @Test
    fun test_音声を合成できる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleVideoFolder = appContext.getExternalFilesDir(null)!!.resolve("sample")

        val resultFile = appContext.getExternalFilesDir(null)!!.resolve("test_音声を合成できる_${System.currentTimeMillis()}.aac").apply { createNewFile() }
        val videoFile = sampleVideoFolder.resolve("iphone.mp4")
        val bgmFile = sampleVideoFolder.resolve("famipop.mp3")

        provideTempFolder { tempFolder ->
            val videoPcm = tempFolder.resolve("video_pcm")
            val bgmPcm = tempFolder.resolve("bgm_pcm")
            val outPcm = tempFolder.resolve("out_pcm")
            // デコード
            AudioEncodeDecodeProcessor.decode(videoFile.toAkariCoreInputDataSource(), videoPcm)
            AudioEncodeDecodeProcessor.decode(bgmFile.toAkariCoreInputDataSource(), bgmPcm)

            // 合成する
            AudioMixingProcessor.start(
                outPcmFile = outPcm,
                durationMs = 10_000,
                mixList = listOf(
                    AudioMixingProcessor.MixAudioData(videoPcm, 0, 10_000),
                    AudioMixingProcessor.MixAudioData(bgmPcm, 0, 10_000)
                )
            )
            // エンコード
            AudioEncodeDecodeProcessor.encode(outPcm, resultFile)
        }
    }

    @Test
    fun test_キャンバスの入力から動画を作成() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val resultFile = File(appContext.getExternalFilesDir(null), "test_キャンバスの入力から動画を作成_${System.currentTimeMillis()}.mp4").apply { createNewFile() }

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 80f
        }

        CanvasVideoProcessor.start(
            resultFile = resultFile,
            codecName = MediaFormat.MIMETYPE_VIDEO_AVC,
            containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
            bitRate = 1_000_000,
            frameRate = 30,
            outputVideoWidth = 1280,
            outputVideoHeight = 720,
        ) { positionMs ->
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
            positionMs <= 10 * 1_000
        }
    }

    @Test
    fun test_動画の切り取りが出来る() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleVideoFolder = appContext.getExternalFilesDir(null)!!.resolve("sample")
        val resultFile = File(appContext.getExternalFilesDir(null), "test_動画の切り取りが出来る_${System.currentTimeMillis()}.mp4").apply { createNewFile() }
        val toomo = sampleVideoFolder.resolve("iphone.mp4")
        // カットしてみる
        CutProcessor.start(toomo.toAkariCoreInputDataSource(), resultFile, 0L..2_000L, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)
    }

    @Test
    fun test_無音の音声が作成できる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val resultFile = File(appContext.getExternalFilesDir(null), "test_無音の音声が作成できる_${System.currentTimeMillis()}.aac").apply { createNewFile() }
        provideTempFolder { tempFolder ->
            val silencePcm = tempFolder.resolve("silence_pcm")
            // 10 秒間の無音の音声ファイルを作る
            SilenceAudioProcessor.start(silencePcm, 10_000)
            // エンコードする
            AudioEncodeDecodeProcessor.encode(silencePcm, resultFile)
        }
    }

    @Test
    fun test_サンプリングレートを8000から48000に変換できる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleVideoFolder = appContext.getExternalFilesDir(null)!!.resolve("sample")
        val resultFile = File(appContext.getExternalFilesDir(null), "test_サンプリングレートを8000から48000に変換できる_${System.currentTimeMillis()}.aac").apply { createNewFile() }
        val bgmFile = sampleVideoFolder.resolve("voice.wav")

        provideTempFolder { tempFolder ->
            val pcmFile = tempFolder.resolve("pcm_file")
            val resamplingPcmFile = tempFolder.resolve("resampling_pcm_file")
            // デコード
            AudioEncodeDecodeProcessor.decode(bgmFile.toAkariCoreInputDataSource(), pcmFile)
            // アップサンプリング
            ReSamplingRateProcessor.reSamplingBySonic(
                inPcmFile = pcmFile,
                outPcmFile = resamplingPcmFile,
                channelCount = 1,
                inSamplingRate = 8_000,
                outSamplingRate = 44_100
            )
            // エンコード
            AudioEncodeDecodeProcessor.encode(
                inPcmFile = resamplingPcmFile,
                outAudioFile = resultFile,
                channelCount = 1
            )
        }
    }

    @Test
    fun test_PCMファイルの音量調整ができる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val sampleVideoFolder = appContext.getExternalFilesDir(null)!!.resolve("sample")
        val resultFile = File(appContext.getExternalFilesDir(null), "test_PCMファイルの音量調整ができる_${System.currentTimeMillis()}.aac").apply { createNewFile() }
        // TODO 音声カットしような
        val bgmFile = sampleVideoFolder.resolve("famipop.mp3")

        provideTempFolder { tempFolder ->
            val pcmFile = tempFolder.resolve("pcm_file")
            val applyVolumePcmFile = tempFolder.resolve("apply_volume_pcm_file")
            // デコード
            AudioEncodeDecodeProcessor.decode(bgmFile.toAkariCoreInputDataSource(), pcmFile)
            // 音量調整
            AudioVolumeProcessor.start(pcmFile, applyVolumePcmFile, 0.05f)
            // エンコード
            AudioEncodeDecodeProcessor.encode(applyVolumePcmFile, resultFile)
        }
    }

    @Test
    fun test_動画のフレームの取得ができる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val videoFrameFileName = listOf(
            "test_動画のフレームの取得ができる_1_000ms.png",
            "test_動画のフレームの取得ができる_15_000ms.png",
            "test_動画のフレームの取得ができる_9_000ms.png"
        ).map { fileName ->
            appContext.getExternalFilesDir(null)!!.resolve(fileName)
        }

        // 仮の動画をつくる。長めに
        val TEMP_VIDEO_LENGTH_MS = 20_0000
        provideTempFolder { tempFolder ->
            val demoVideoFile = tempFolder.resolve("demo_video.mp4")
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 80f
            }
            CanvasVideoProcessor.start(
                resultFile = demoVideoFile,
                codecName = MediaFormat.MIMETYPE_VIDEO_AVC,
                containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                bitRate = 1_000_000,
                frameRate = 30,
                outputVideoWidth = 1280,
                outputVideoHeight = 720
            ) { positionMs ->
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
                positionMs <= TEMP_VIDEO_LENGTH_MS
            }

            // 動画のフレーム取得
            val videoFrameBitmapExtractor = VideoFrameBitmapExtractor().apply {
                prepareDecoder(demoVideoFile.toAkariCoreInputDataSource())
            }

            // 巻き戻すやつもテストしたいから 1 -> 15 -> 9 で
            val videoFrame_1000 = videoFrameBitmapExtractor.getVideoFrameBitmap(seekToMs = 1_000)
            val videoFrame_15_000 = videoFrameBitmapExtractor.getVideoFrameBitmap(seekToMs = 15_000)
            val videoFrame_9000 = videoFrameBitmapExtractor.getVideoFrameBitmap(seekToMs = 9000)
            // 保存
            videoFrame_1000.compress(Bitmap.CompressFormat.PNG, 100, videoFrameFileName[0].outputStream())
            videoFrame_15_000.compress(Bitmap.CompressFormat.PNG, 100, videoFrameFileName[1].outputStream())
            videoFrame_9000.compress(Bitmap.CompressFormat.PNG, 100, videoFrameFileName[2].outputStream())
        }
    }

    /** 一時的なファイル置き場を作る。ブロックを抜けたら削除されます。 */
    private suspend fun provideTempFolder(action: suspend (tempFolder: File) -> Unit) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val tempFolder = File(appContext.getExternalFilesDir(null), "temp").apply {
            deleteRecursively()
            mkdir()
        }
        try {
            action(tempFolder)
        } finally {
            tempFolder.deleteRecursively()
        }
    }

    companion object {
        /** runTest デフォルトタイムアウト */
        private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L
    }
}