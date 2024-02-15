package io.github.takusan23.akaridroid

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.v2.video.CanvasVideoProcessor
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import io.github.takusan23.akaridroid.v2.canvasrender.CanvasRender
import io.github.takusan23.akaridroid.v2.RenderData
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

/** [CanvasRender] のテスト */
@RunWith(AndroidJUnit4::class)
class CanvasRenderTest {

    private val renderData = RenderData(
        durationMs = 10_000,
        videoSize = RenderData.Size(1280, 720),
        canvasRenderItem = emptyList(),
        audioRenderItem = emptyList()
    )

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

    @Test
    fun test_テキストのRenderDataから動画を作る() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        encode(
            testName = "test_テキストのRenderDataから動画を作る",
            canvasRender = CanvasRender(targetContext).apply {
                setRenderData(
                    canvasRenderItem = listOf(
                        RenderData.CanvasItem.Text(
                            displayTime = RenderData.DisplayTime(0, 10_000),
                            position = RenderData.Position(100f, 100f),
                            text = "こんにちは",
                            fontColor = "#ffffff",
                            textSize = 100f
                        ),
                        RenderData.CanvasItem.Text(
                            displayTime = RenderData.DisplayTime(0, 10_000),
                            position = RenderData.Position(100f, 300f),
                            text = "あかりどろいど",
                            fontColor = "#ff0000"
                        ),
                        RenderData.CanvasItem.Text(
                            displayTime = RenderData.DisplayTime(3_000, 5_000),
                            position = RenderData.Position(100f, 700f),
                            text = "3 ～ 5 の間しかでない文字",
                            fontColor = "#ff0000"
                        )
                    )
                )
            }
        )
    }

    @Test
    fun test_画像のRenderDataから動画を作る() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val imagePath = createFile("temp_photo").also { file ->
            file.outputStream().use { outputStream ->
                ContextCompat.getDrawable(targetContext, R.drawable.outline_audiotrack_24)!!
                    .apply { setTintList(ColorStateList.valueOf(Color.WHITE)) }
                    .toBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
        encode(
            testName = "test_画像のRenderDataから動画を作る",
            canvasRender = CanvasRender(targetContext).apply {
                setRenderData(
                    canvasRenderItem = listOf(
                        RenderData.CanvasItem.Image(
                            displayTime = RenderData.DisplayTime(0, 10_000),
                            position = RenderData.Position(100f, 100f),
                            filePath = imagePath.path,
                            size = RenderData.Size(100, 100)
                        ),
                        RenderData.CanvasItem.Image(
                            displayTime = RenderData.DisplayTime(0, 10_000),
                            position = RenderData.Position(300f, 300f),
                            filePath = imagePath.path,
                            size = RenderData.Size(300, 300)
                        )
                    )
                )
            }
        )
        imagePath.delete()
    }

    @Test
    fun test_動画のRenderDataから動画を作る() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        // TODO あらかじめ app/src/androidTest/res/raw/test_toomo.mp4 ファイルを置いておく
        // File しか受け付けないのでとりあえずコピー
        val testToomoMp4 = createFile("test_toomo").also { testToomoMp4 ->
            testToomoMp4.outputStream().use { outputStream ->
                context.resources
                    .openRawResource(io.github.takusan23.akaridroid.test.R.raw.test_toomo)
                    .copyTo(outputStream)
            }
        }
        encode(
            testName = "test_動画のRenderDataから動画を作る",
            durationMs = 10_000,
            canvasRender = CanvasRender(targetContext).apply {
                setRenderData(
                    canvasRenderItem = listOf(
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(0, 10_000),
                            position = RenderData.Position(0f, 0f),
                            filePath = testToomoMp4.path,
                            size = RenderData.Size(640, 360)
                        ),
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(0, 10_000),
                            position = RenderData.Position(640f, 0f),
                            filePath = testToomoMp4.path,
                            size = RenderData.Size(640, 360)
                        ),
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(0, 10_000),
                            position = RenderData.Position(0f, 360f),
                            filePath = testToomoMp4.path,
                            size = RenderData.Size(640, 360)
                        ),
                        RenderData.CanvasItem.Text(
                            displayTime = RenderData.DisplayTime(0, 10_000),
                            position = RenderData.Position(640f, 500f),
                            text = "こんにちは",
                            fontColor = "#ffffff",
                            textSize = 100f
                        )
                    )
                )
            }
        )
        testToomoMp4.delete()
    }

    /** [CanvasRender]を渡したらエンコードして動画フォルダに保存してくれるやつ */
    private suspend fun encode(
        testName: String,
        canvasRender: CanvasRender,
        durationMs: Long = renderData.durationMs
    ) {
        val resultFile = createFile(testName)
        CanvasVideoProcessor.start(
            resultFile = resultFile,
            outputVideoHeight = renderData.videoSize.height,
            outputVideoWidth = renderData.videoSize.width,
            onCanvasDrawRequest = { positionMs ->
                canvasRender.draw(
                    canvas = this,
                    durationMs = durationMs,
                    currentPositionMs = positionMs
                )
                positionMs < durationMs
            }
        )
        // なんかしらんけど getExternalFilesDir 消えるので MediaStore にブチこむ
        MediaStoreTool.copyToVideoFolder(targetContext, resultFile)
        resultFile.delete()
    }

    private fun createFile(name: String) = targetContext.getExternalFilesDir(null)!!.resolve("${name}_${System.currentTimeMillis()}.mp4").apply {
        createNewFile()
    }

    companion object {
        /** runTest デフォルトタイムアウト */
        private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L
    }

}