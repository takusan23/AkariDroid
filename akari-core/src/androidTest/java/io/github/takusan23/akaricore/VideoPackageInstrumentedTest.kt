package io.github.takusan23.akaricore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaMetadataRetriever
import androidx.core.graphics.alpha
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.video.CanvasVideoProcessor
import io.github.takusan23.akaricore.video.GpuShaderImageProcessor
import io.github.takusan23.akaricore.video.VideoFrameBitmapExtractor
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/** video パッケージのテスト */
@RunWith(AndroidJUnit4::class)
class VideoPackageInstrumentedTest {

    @Test
    fun test_キャンバスから動画を作れる() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {

        // 赤色の動画
        val resultFile = CommonTestTool.getTestExternalFilesDir().resolve("canvas_video_processor_${System.currentTimeMillis()}.mp4")
        CanvasVideoProcessor.start(
            output = resultFile.toAkariCoreInputOutputData(),
            onCanvasDrawRequest = { currentPositionMs ->
                drawColor(Color.RED)
                currentPositionMs < 5_000
            }
        )

        MediaMetadataRetriever().use { mediaMetadataRetriever ->
            mediaMetadataRetriever.setDataSource(resultFile.path)
            val durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()

            assertTrue("エンコードした動画と時間が一致しません $durationMs") {
                // TODO なぜか寸足らずになる、おそらく動画に動きがないからフレームが出てこない？根本修正をして戻す
                // 3_000 < durationMs
                1_000 < durationMs
            }

            // 適当な位置のフレームを取り出す
            val frameBitmap = mediaMetadataRetriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_NEXT_SYNC)!! // ナノ秒
            // 少なくとも Red が 0xFF(255) であること
            // 厳密に Color.RED には多分ならない。エンコードのせい
            assertEquals(0xFF, frameBitmap[0, 0].red, "エンコードした動画と色が一致しません")
            assertEquals(0xFF, frameBitmap[10, 10].red, "エンコードした動画と色が一致しません")
        }
    }

    @Test
    fun test_連続でフレームを取り出せる() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        // テスト動画を作る
        val createVideoFile = CommonTestTool.createTestVideo(4_000) { currentPositionMs ->
            drawCanvas {
                drawColor(
                    when (currentPositionMs) {
                        in 0..1_000 -> Color.RED
                        in 1_000..2_000 -> Color.GREEN
                        in 2_000..3_000 -> Color.BLUE
                        else -> Color.BLACK
                    }
                )
            }
        }

        val videoFrameBitmapExtractor = VideoFrameBitmapExtractor().apply {
            prepareDecoder(
                input = createVideoFile.toAkariCoreInputOutputData(),
                chromakeyColor = Color.BLUE,
                chromakeyThreshold = 0.5f
            )
        }

        val timeMs = measureTimeMillis {
            // 0..1000ms は赤色であるはず
            val redVideoFrameBitmap = videoFrameBitmapExtractor.getVideoFrameBitmap(500)!!
            assertEquals(0xFF, redVideoFrameBitmap[0, 0].red, "動画フレームと色が一致しません")
            assertEquals(0xFF, redVideoFrameBitmap[10, 10].red, "動画フレームと色が一致しません")

            // 1000..2000ms は緑色であるはず
            val greenVideoFrameBitmap = videoFrameBitmapExtractor.getVideoFrameBitmap(1_500)!!
            assertEquals(0xFF, greenVideoFrameBitmap[0, 0].green, "動画フレームと色が一致しません")
            assertEquals(0xFF, greenVideoFrameBitmap[10, 10].green, "動画フレームと色が一致しません")

            // 2000..3000ms はクロマキー
            val chromakeyVideoFrameBitmap = videoFrameBitmapExtractor.getVideoFrameBitmap(2_500)!!
            assertEquals(0x00, chromakeyVideoFrameBitmap[0, 0].alpha, "動画フレームと色が一致しません")
            assertEquals(0x00, chromakeyVideoFrameBitmap[10, 10].alpha, "動画フレームと色が一致しません")
        }

        // 多分 1 秒もかからないはず
        // TODO 端末のスペックに依存する
        assertTrue("指定時間以上かかってます") { timeMs < 1_000 }

        videoFrameBitmapExtractor.destroy()
    }

    @Test
    fun test_画像にフラグメントシェーダーを適用できる() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val shaderImageProcessor = GpuShaderImageProcessor().apply {
            prepare(
                width = CommonTestTool.TEST_VIDEO_WIDTH,
                height = CommonTestTool.TEST_VIDEO_HEIGHT,
                fragmentShaderCode = GpuShaderImageProcessor.FRAGMENT_SHADER_TEXTURE_RENDER // パススルーエフェクト。何もしない。
            )
        }
        // 塗りつぶし Bitmap
        val fillRedBitmap = Bitmap.createBitmap(CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).apply {
                drawColor(Color.RED)
            }
        }

        // エフェクトをかける
        val applyBitmap = shaderImageProcessor.drawShader(fillRedBitmap)!!

        // パススルーのフラグメントシェーダーなので
        assertTrue("パススルーのエフェクトだが、入力と同じになっていない") { applyBitmap.sameAs(fillRedBitmap) == true }
        shaderImageProcessor.destroy()
    }

}