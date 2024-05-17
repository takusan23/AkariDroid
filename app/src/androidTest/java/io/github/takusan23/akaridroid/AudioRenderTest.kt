package io.github.takusan23.akaridroid

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaridroid.audiorender.AudioRender
import io.github.takusan23.akaridroid.test.R
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/** [AudioRender]のテスト */
class AudioRenderTest {

    /**
     * あかりどろいどの Context
     * app/src/android/res/
     */
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * あかりどろいどインストゥルメンタルテストの Context
     * app/src/main/res/ にアクセスする必要があれば
     */
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    /** 作業用フォルダ */
    private val tempFolder: File
        get() = targetContext.getExternalFilesDir(null)!!.resolve(TEMP_FOLDER).apply { mkdir() }

    @Test
    fun test_音声の作成ができる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        // ファイルを用意
        val testToomoMp4 = copyTestFileFromRawFolder()
        // 作る
        val resultPcm = createTempFile("result_pcm")
        AudioRender(
            context = context,
            outputDecodePcmFolder = tempFolder,
            outPcmFile = resultPcm,
            tempFolder = tempFolder
        ).apply {
            setRenderData(
                audioRenderItem = listOf(
                    RenderData.AudioItem.Audio(
                        displayTime = RenderData.DisplayTime(0, 10_000),
                        layerIndex = 0,
                        filePath = RenderData.FilePath.File(testToomoMp4.path)
                    )
                ),
                durationMs = 10_000
            )
        }
        // エンコードする
        encodeAndSaveAudio(resultPcm, "test_音声の作成ができる.aac")
        // 消す
        tempFolder.deleteRecursively()
    }

    @Test
    fun test_音声の連結ができる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        // ファイルを用意
        val testToomoMp4 = copyTestFileFromRawFolder()
        // 作る
        val resultPcm = createTempFile("result_pcm")
        AudioRender(
            context = context,
            outputDecodePcmFolder = tempFolder,
            outPcmFile = resultPcm,
            tempFolder = tempFolder
        ).apply {
            setRenderData(
                audioRenderItem = listOf(
                    RenderData.AudioItem.Audio(
                        displayTime = RenderData.DisplayTime(0, 3_000),
                        layerIndex = 0,
                        filePath = RenderData.FilePath.File(testToomoMp4.path),
                        volume = 0.1f
                    ),
                    RenderData.AudioItem.Audio(
                        displayTime = RenderData.DisplayTime(3_000, 6_000),
                        layerIndex = 0,
                        filePath = RenderData.FilePath.File(testToomoMp4.path),
                        volume = 0.5f
                    ),
                    RenderData.AudioItem.Audio(
                        displayTime = RenderData.DisplayTime(6_000, 10_000),
                        layerIndex = 0,
                        filePath = RenderData.FilePath.File(testToomoMp4.path),
                        volume = 1f
                    )
                ),
                durationMs = 10_000
            )
        }
        // エンコードする
        encodeAndSaveAudio(resultPcm, "test_音声の連結ができる")
        // 消す
        tempFolder.deleteRecursively()
    }

    @Test
    fun test_音声の重ね合わせができる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        // ファイルを用意
        val testToomoMp4 = copyTestFileFromRawFolder()
        // 作る
        val resultPcm = createTempFile("result_pcm")
        AudioRender(
            context = context,
            outputDecodePcmFolder = tempFolder,
            outPcmFile = resultPcm,
            tempFolder = tempFolder
        ).apply {
            setRenderData(
                // ちょっとずらす
                // 音割れ？ 0xFFFF を超えるかも？ので音量を下げる
                audioRenderItem = (0 until 10).map { index ->
                    RenderData.AudioItem.Audio(
                        displayTime = RenderData.DisplayTime(index * 1_000L, 10_000L),
                        layerIndex = 0,
                        filePath = RenderData.FilePath.File(testToomoMp4.path),
                        volume = 0.1f
                    )
                },
                durationMs = 10_000
            )
        }
        // エンコードする
        encodeAndSaveAudio(resultPcm, "test_音声の重ね合わせができる")
        // 消す
        tempFolder.deleteRecursively()
    }

    @Test
    fun test_音声の速度変更ができる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        // ファイルを用意
        val testToomoMp4 = copyTestFileFromRawFolder()
        // 作る
        val resultPcm = createTempFile("result_pcm")
        AudioRender(
            context = context,
            outputDecodePcmFolder = tempFolder,
            outPcmFile = resultPcm,
            tempFolder = tempFolder
        ).apply {
            setRenderData(
                audioRenderItem = listOf(
                    // 2倍速
                    RenderData.AudioItem.Audio(
                        displayTime = RenderData.DisplayTime(0, 10_000L),
                        layerIndex = 0,
                        filePath = RenderData.FilePath.File(testToomoMp4.path),
                        playbackSpeed = 2f
                    ),
                    // 0.5倍速
                    RenderData.AudioItem.Audio(
                        displayTime = RenderData.DisplayTime(5_000L, 10_000L),
                        layerIndex = 0,
                        filePath = RenderData.FilePath.File(testToomoMp4.path),
                        playbackSpeed = 0.5f
                    )
                ),
                durationMs = 10_000L
            )
        }
        // エンコードする
        encodeAndSaveAudio(resultPcm, "test_音声の速度変更ができる")
        // 消す
        tempFolder.deleteRecursively()
    }

    /** app/src/androidTest/res/raw/ をコピーする */
    private suspend fun copyTestFileFromRawFolder(): File = withContext(Dispatchers.IO) {
        createTempFile("test_toomo").also { testToomoMp4 ->
            testToomoMp4.outputStream().use { outputStream ->
                context.resources
                    .openRawResource(R.raw.test_toomo)
                    .copyTo(outputStream)
            }
        }
    }

    /** エンコードして、端末の音声フォルダへ保存する */
    private suspend fun encodeAndSaveAudio(pcmFile: File, testName: String) {
        val outFile = createTempFile("${testName}_${System.currentTimeMillis()}.aac")
        AudioEncodeDecodeProcessor.encode(
            input = pcmFile.toAkariCoreInputOutputData(),
            output = outFile.toAkariCoreInputOutputData()
        )
        MediaStoreTool.copyToAudioFolder(context, outFile)
    }

    /** 仮のファイル作る。テスト終わったら削除される */
    private fun createTempFile(fileName: String): File = tempFolder.resolve("${fileName}_${System.currentTimeMillis()}").apply {
        createNewFile()
    }

    companion object {
        private const val TEMP_FOLDER = "temp_folder"

        /** runTest デフォルトタイムアウト */
        private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L
    }
}