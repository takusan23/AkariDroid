package io.github.takusan23.akaridroid

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.video.CanvasVideoProcessor
import io.github.takusan23.akaricore.video.GpuShaderImageProcessor
import io.github.takusan23.akaridroid.canvasrender.CanvasRender
import io.github.takusan23.akaridroid.tool.MediaStoreTool
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
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(100f, 100f),
                            layerIndex = 0,
                            text = "枠取り文字",
                            fontColor = "#000000",
                            strokeColor = "#ffffff",
                            textSize = 100f
                        ),
                        RenderData.CanvasItem.Text(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(100f, 300f),
                            layerIndex = 0,
                            text = "あかりどろいど",
                            fontColor = "#ff0000"
                        ),
                        RenderData.CanvasItem.Text(
                            displayTime = RenderData.DisplayTime(startMs = 3_000, durationMs = 5_000),
                            position = RenderData.Position(100f, 700f),
                            layerIndex = 0,
                            text = "3 ～ 5 の間しかでない文字",
                            fontColor = "#ff0000"
                        ),
                        RenderData.CanvasItem.Text(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(500f, 300f),
                            layerIndex = 0,
                            text = """
                                複数行
                                の文字でも
                                動くように、
                                Canvas#drawText
                                を
                                行の分だけ呼んでいます。
                            """.trimIndent(),
                            fontColor = "#ffffff",
                            textSize = 50f
                        ),
                    )
                )
            }
        )
    }

    @Test
    fun test_画像のRenderDataから動画を作る() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val imagePath = createFile("temp_photo").also { file ->
            file.outputStream().use { outputStream ->
                ContextCompat.getDrawable(targetContext, R.drawable.ic_outline_audiotrack_24)!!
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
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(100f, 100f),
                            filePath = RenderData.FilePath.File(imagePath.path),
                            layerIndex = 0,
                            size = RenderData.Size(100, 100)
                        ),
                        RenderData.CanvasItem.Image(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(300f, 300f),
                            filePath = RenderData.FilePath.File(imagePath.path),
                            layerIndex = 0,
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
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(0f, 0f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(640, 360)
                        ),
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(640f, 0f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(640, 360)
                        ),
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(0f, 360f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(640, 360)
                        ),
                        RenderData.CanvasItem.Text(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(640f, 500f),
                            layerIndex = 0,
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

    @Test
    fun test_動画のRenderDataから動画を作る_オフセット付き() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
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
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(0f, 0f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(640, 360)
                        ),
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(startMs = 4_000, durationMs = 10_000),
                            position = RenderData.Position(640f, 0f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(640, 360),
                            displayOffset = RenderData.DisplayOffset(4_000) // 4秒 スキップ
                        ),
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(startMs = 8_000, durationMs = 10_000),
                            position = RenderData.Position(0f, 360f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(640, 360),
                            displayOffset = RenderData.DisplayOffset(8_000) // 8秒 スキップ
                        ),
                        RenderData.CanvasItem.Text(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(640f, 500f),
                            layerIndex = 0,
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

    @Test
    fun test_図形のRenderDataから動画を作る() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        encode(
            testName = "test_図形のRenderDataから動画を作る",
            canvasRender = CanvasRender(targetContext).apply {
                setRenderData(
                    canvasRenderItem = listOf(
                        RenderData.CanvasItem.Shape(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(0f, 0f),
                            layerIndex = 0,
                            color = "#ffffff",
                            size = RenderData.Size(1280, 720),
                            shapeType = RenderData.CanvasItem.Shape.ShapeType.Rect
                        ),
                        RenderData.CanvasItem.Shape(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(100f, 100f),
                            layerIndex = 0,
                            color = "#0000ff",
                            size = RenderData.Size(100, 100),
                            shapeType = RenderData.CanvasItem.Shape.ShapeType.Circle
                        ),
                        RenderData.CanvasItem.Shape(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(300f, 100f),
                            layerIndex = 0,
                            color = "#ffff00",
                            size = RenderData.Size(100, 100),
                            shapeType = RenderData.CanvasItem.Shape.ShapeType.Circle
                        ),
                        RenderData.CanvasItem.Shape(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(500f, 100f),
                            layerIndex = 0,
                            color = "#ff0000",
                            size = RenderData.Size(100, 100),
                            shapeType = RenderData.CanvasItem.Shape.ShapeType.Circle
                        ),
                        RenderData.CanvasItem.Shape(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(100f, 400f),
                            layerIndex = 0,
                            color = "#ff0000",
                            size = RenderData.Size(100, 100),
                            shapeType = RenderData.CanvasItem.Shape.ShapeType.Rect
                        ),
                        RenderData.CanvasItem.Shape(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(100f, 500f),
                            layerIndex = 0,
                            color = "#0000ff",
                            size = RenderData.Size(100, 100),
                            shapeType = RenderData.CanvasItem.Shape.ShapeType.Rect
                        ),
                    )
                )
            }
        )
    }

    @Test
    fun test_動画のRenderDataから再生速度が変更された動画を作る() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
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
            testName = "test_動画のRenderDataから再生速度が変更された動画を作る",
            durationMs = 10_000,
            canvasRender = CanvasRender(targetContext).apply {
                setRenderData(
                    canvasRenderItem = listOf(
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000, playbackSpeed = 2f),
                            position = RenderData.Position(0f, 0f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(640, 360)
                        ),
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(startMs = 5_000, durationMs = 10_000, playbackSpeed = 0.5f),
                            position = RenderData.Position(640f, 0f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(640, 360)
                        )
                    )
                )
            }
        )
        testToomoMp4.delete()
    }

    @Test
    fun test_各フレームにGLSLのフラグメントシェーダーを通してエフェクトを適用できる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
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
            testName = "test_各フレームにGLSLのフラグメントシェーダーを通してエフェクトを適用できる",
            durationMs = 10_000,
            canvasRender = CanvasRender(targetContext).apply {
                setRenderData(
                    canvasRenderItem = listOf(
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(0f, 0f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(1280, 720)
                        ),
                        RenderData.CanvasItem.Shader(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(0f, 0f),
                            layerIndex = 1,
                            size = RenderData.Size(1280, 720),
                            name = "フラグメントシェーダー",
                            fragmentShader = FRAGMENT_SHADER_MOSAIC
                        )
                    )
                )
            }
        )
        testToomoMp4.delete()
    }

    @Test
    fun test_各フレームにGLSLのフラグメントシェーダーを通してエフェクトを適用できる_部分的に適用() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
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
            testName = "test_各フレームにGLSLのフラグメントシェーダーを通してエフェクトを適用できる_部分的に適用",
            durationMs = 10_000,
            canvasRender = CanvasRender(targetContext).apply {
                setRenderData(
                    canvasRenderItem = listOf(
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(0f, 0f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(1280, 720)
                        ),
                        RenderData.CanvasItem.Shader(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000),
                            position = RenderData.Position(320f, 160f),
                            layerIndex = 1,
                            size = RenderData.Size(640, 320),
                            name = "フラグメントシェーダー",
                            fragmentShader = FRAGMENT_SHADER_MOSAIC
                        )
                    )
                )
            }
        )
        testToomoMp4.delete()
    }

    @Test
    fun test_各フレームにGLSLのフラグメントシェーダーを通してエフェクトを適用できる_時間uniform変数() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
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
            testName = "test_各フレームにGLSLのフラグメントシェーダーを通してエフェクトを適用できる_時間uniform変数",
            durationMs = 3_000,
            canvasRender = CanvasRender(targetContext).apply {
                setRenderData(
                    canvasRenderItem = listOf(
                        RenderData.CanvasItem.Video(
                            displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 3_000),
                            position = RenderData.Position(0f, 0f),
                            layerIndex = 0,
                            filePath = RenderData.FilePath.File(testToomoMp4.path),
                            size = RenderData.Size(1280, 720)
                        ),
                        RenderData.CanvasItem.Shader(
                            displayTime = RenderData.DisplayTime(startMs = 1_000, durationMs = 2_000),
                            position = RenderData.Position(0f, 0f),
                            layerIndex = 1,
                            size = RenderData.Size(1280, 720),
                            name = "フラグメントシェーダー",
                            fragmentShader = FRAGMENT_SHADER_FADE
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
            output = resultFile.toAkariCoreInputOutputData(),
            outputVideoHeight = renderData.videoSize.height,
            outputVideoWidth = renderData.videoSize.width,
            onCanvasDrawRequest = { positionMs ->
                canvasRender.draw(
                    outCanvas = this,
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

        /**
         * 画像を表示するだけだとわからんので、モザイクしてみる
         * https://qiita.com/edo_m18/items/d166653ac0dccbc607dc
         *
         * uniform 変数は[GpuShaderImageProcessor]参照。
         */
        private const val FRAGMENT_SHADER_MOSAIC = """precision mediump float;

uniform sampler2D s_texture;
uniform vec2 v_resolution;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / v_resolution.xy;
    // 反転しているので
    uv = vec2(uv.x, 1.-uv.y);
    // モザイクしてみる
    uv = floor(uv * 15.0) / 15.0;
    // 色を出す
    vec4 color = texture2D(s_texture, uv);
    gl_FragColor = color;
}
"""

        /** f_time uniform 変数を使ったフラグメントシェーダー */
        private const val FRAGMENT_SHADER_FADE = """precision mediump float;

uniform sampler2D s_texture;
uniform vec2 v_resolution;
uniform float f_time;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / v_resolution.xy;
    // 反転しているので
    uv = vec2(uv.x, 1.-uv.y);
    // 色を出す
    vec4 color = texture2D(s_texture, uv);
    // フェードする
    color.rgb *= 1.0 - f_time;
    gl_FragColor = color;
}
"""

    }

}