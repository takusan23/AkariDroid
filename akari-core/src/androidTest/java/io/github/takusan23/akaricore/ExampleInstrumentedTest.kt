package io.github.takusan23.akaricore

import android.media.MediaFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.processor.AudioMixingProcessor
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
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /**
     * 音声合成ができるのかテスト
     *
     * 保存先は /storage/emulated/0/Android/data/io.github.takusan23.akari_core.test/files
     */
    @OptIn(ExperimentalCoroutinesApi::class)
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

    companion object {
        /** runTest デフォルトタイムアウト */
        private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L
    }
}