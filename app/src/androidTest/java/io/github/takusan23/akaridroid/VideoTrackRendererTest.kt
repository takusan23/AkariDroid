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
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoEncoder
import io.github.takusan23.akaridroid.canvasrender.VideoTrackRenderer
import io.github.takusan23.akaridroid.canvasrender.itemrender.EffectRenderer
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

/** [VideoTrackRenderer] のテスト。といいつつチェックは生成された動画を目視でやっている。何か自動化する方法はないかな。 */
@RunWith(AndroidJUnit4::class)
class VideoTrackRendererTest {

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
                    displayTime = RenderData.DisplayTime(startMs = 3_000, durationMs = 3_000),
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
            testName = "test_動画のRenderDataから動画を作る_オフセット付き",
            durationMs = 10_000,
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
        testToomoMp4.delete()
    }

    @Test
    fun test_図形のRenderDataから動画を作る() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        encode(
            testName = "test_図形のRenderDataから動画を作る",
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
            canvasRenderItem = listOf(
                RenderData.CanvasItem.Video(
                    displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000, playbackSpeed = 2f),
                    position = RenderData.Position(0f, 0f),
                    layerIndex = 0,
                    filePath = RenderData.FilePath.File(testToomoMp4.path),
                    size = RenderData.Size(640, 360)
                ),
                RenderData.CanvasItem.Video(
                    displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 10_000, playbackSpeed = 0.5f),
                    position = RenderData.Position(640f, 0f),
                    layerIndex = 0,
                    filePath = RenderData.FilePath.File(testToomoMp4.path),
                    size = RenderData.Size(640, 360)
                )
            )
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
                    fragmentShader = EffectRenderer.FRAGMENT_SHADER_MOSAIC // EffectRender から借りてくる
                )
            )
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
                    fragmentShader = EffectRenderer.FRAGMENT_SHADER_MOSAIC
                )
            )
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
        testToomoMp4.delete()
    }

    @Test
    fun test_切り替えアニメーションが動く() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
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
            testName = "test_切り替えアニメーションが動く",
            durationMs = 3_000,
            canvasRenderItem = listOf(
                RenderData.CanvasItem.Video(
                    displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 3_000),
                    position = RenderData.Position(0f, 0f),
                    layerIndex = 0,
                    filePath = RenderData.FilePath.File(testToomoMp4.path),
                    size = RenderData.Size(1280, 720)
                ),
                RenderData.CanvasItem.SwitchAnimation(
                    displayTime = RenderData.DisplayTime(startMs = 1_000, durationMs = 2_000),
                    position = RenderData.Position(0f, 0f),
                    layerIndex = 1,
                    size = RenderData.Size(1280, 720),
                    animationType = RenderData.CanvasItem.SwitchAnimation.SwitchAnimationType.BLUR
                )
            )
        )
        testToomoMp4.delete()
    }

    @Test
    fun test_エフェクトが適用できる() = runTest(timeout = (DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
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
            testName = "test_エフェクトが適用できる",
            durationMs = 3_000,
            canvasRenderItem = listOf(
                RenderData.CanvasItem.Video(
                    displayTime = RenderData.DisplayTime(startMs = 0, durationMs = 3_000),
                    position = RenderData.Position(0f, 0f),
                    layerIndex = 0,
                    filePath = RenderData.FilePath.File(testToomoMp4.path),
                    size = RenderData.Size(1280, 720)
                ),
                RenderData.CanvasItem.Effect(
                    displayTime = RenderData.DisplayTime(startMs = 1_000, durationMs = 2_000),
                    position = RenderData.Position(640f, 0f),
                    layerIndex = 1,
                    size = RenderData.Size(640, 720),
                    effectType = RenderData.CanvasItem.Effect.EffectType.THRESHOLD
                )
            )
        )
        testToomoMp4.delete()
    }


    /** [VideoTrackRenderer]で[canvasRenderItem]を渡してエンコードして動画フォルダに保存してくれるやつ */
    private suspend fun encode(
        testName: String,
        canvasRenderItem: List<RenderData.CanvasItem>,
        durationMs: Long = renderData.durationMs
    ) {
        val (width, height) = renderData.videoSize
        val resultFile = createFile(testName)
        val akariVideoEncoder = AkariVideoEncoder().apply {
            prepare(
                output = resultFile.toAkariCoreInputOutputData(),
                outputVideoHeight = height,
                outputVideoWidth = width,
            )
        }
        val videoTrackRenderer = VideoTrackRenderer(targetContext).apply {
            setOutputSurface(akariVideoEncoder.getInputSurface())
            setVideoParameters(width, height, isEnableTenBitHdr = false)
            setRenderData(canvasRenderItem)
        }
        try {
            // 動画フレーム作成
            coroutineScope {
                val encoderJob = launch { akariVideoEncoder.start() }
                val graphicsJob = launch {
                    videoTrackRenderer.drawRecordLoop(
                        durationMs = durationMs,
                        frameRate = 30,
                        onProgress = { /* do nothing */ }
                    )
                }
                // 終わったらエンコーダーも終了
                graphicsJob.join()
                encoderJob.cancelAndJoin()
            }
            // なんかしらんけど getExternalFilesDir 消えるので MediaStore にブチこむ
            MediaStoreTool.copyToVideoFolder(targetContext, resultFile)
        } finally {
            resultFile.delete()
        }
    }

    private fun createFile(name: String) = targetContext.getExternalFilesDir(null)!!.resolve("${name}_${System.currentTimeMillis()}.mp4").apply {
        createNewFile()
    }

    companion object {
        /** runTest デフォルトタイムアウト */
        private const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L

        /** f_time uniform 変数を使ったフラグメントシェーダー */
        private const val FRAGMENT_SHADER_FADE = """precision mediump float;

uniform sampler2D sVideoFrameTexture;
uniform vec2 vResolution;
uniform vec4 vCropLocation;
uniform float f_time;

void main() {
    vec4 fragCoord = gl_FragCoord;
    // 正規化する
    vec2 uv = fragCoord.xy / vResolution.xy;
    // 色を出す
    vec4 color = texture2D(sVideoFrameTexture, uv);
    // フェードする
    // 範囲内
    if (vCropLocation[0] < uv.x && vCropLocation[1] > uv.x && vCropLocation[2] < uv.y && vCropLocation[3] > uv.y) {
        color.rgb *= 1.0 - f_time;
    }
    gl_FragColor = color;
}
"""

    }

}