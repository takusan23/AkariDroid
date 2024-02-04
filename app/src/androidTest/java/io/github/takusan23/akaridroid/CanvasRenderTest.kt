package io.github.takusan23.akaridroid

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.v2.video.CanvasVideoProcessor
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import io.github.takusan23.akaridroid.v2.canvasrender.CanvasRender
import io.github.takusan23.akaridroid.v2.canvasrender.RenderData
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

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun test_テキストのみのRenderDataから動画を作る() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val resultFile = createResultFile("test_テキストのみのRenderDataから動画を作る")
        val canvasRender = CanvasRender(context).apply {
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
        CanvasVideoProcessor.start(
            resultFile = resultFile,
            outputVideoHeight = renderData.videoSize.height,
            outputVideoWidth = renderData.videoSize.width,
            onCanvasDrawRequest = { positionMs ->
                canvasRender.draw(this, positionMs)
                positionMs < renderData.durationMs
            }
        )
        // なんかしらんけど getExternalFilesDir 消えるので MediaStore にブチこむ
        MediaStoreTool.copyToVideoFolder(context, resultFile)
        resultFile.delete()
    }

    private fun createResultFile(name: String) = context.getExternalFilesDir(null)!!.resolve("${name}_${System.currentTimeMillis()}.mp4").apply {
        createNewFile()
    }

    companion object {
        /** runTest デフォルトタイムアウト */
        private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L
    }

}