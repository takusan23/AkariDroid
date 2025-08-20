package io.github.takusan23.akaricore

import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.media.ImageReader
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.HandlerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.takusan23.akaricore.CommonTestTool.TEST_VIDEO_FRAME_MS
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoDecoder
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

@RunWith(AndroidJUnit4::class)
class VideoEncodeDecodeTest {

    @Test
    fun test_動画のエンコードが出来る() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        val simpleDateFormat = SimpleDateFormat("MM:ss")
        val paint = Paint().apply {
            color = Color.RED
            textSize = 50f
        }
        val durationSec = 3
        val testFile = CommonTestTool.createTestVideo(
            durationMs = durationSec * 1_000L,
            onDrawRequest = {
                drawCanvas {
                    drawText(simpleDateFormat.format(it), 100f, 100f, paint)
                }
            }
        )
        // 動画の時間と、フレームレート分フレームレートが存在すること
        MediaMetadataRetriever().use {
            it.setDataSource(testFile.path)
            val frameCount = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)!!.toInt()
            val durationMs = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toInt()
            // 30fps だと 33ms ごとに刻んでいるため、equals の比較は難しい、ので単純に大きくなってるかだけを見ている
            assertTrue { CommonTestTool.TEST_VIDEO_FPS * durationSec < frameCount }
            assertTrue { durationSec * 1_000L <= durationMs }
        }
    }

    @Test
    fun test_動画のデコードが出来る() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        // テスト動画作成
        val simpleDateFormat = SimpleDateFormat("MM:ss")
        val paint = Paint().apply {
            color = Color.RED
            textSize = 50f
        }
        val durationSec = 3
        val testFile = CommonTestTool.createTestVideo(
            durationMs = durationSec * 1_000L,
            onDrawRequest = {
                drawCanvas {
                    drawText(simpleDateFormat.format(it), 100f, 100f, paint)
                }
            }
        )

        // デコーダー
        val imageReader = ImageReader.newInstance(CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, ImageFormat.PRIVATE, 32)
        val videoDecoder = AkariVideoDecoder().apply {
            prepare(
                input = testFile.toAkariCoreInputOutputData(),
                outputSurface = imageReader.surface
            )
        }

        // フレームを数える
        var frameCount = 0
        val handler = Handler(HandlerThread("HandlerThreadDispatcher").apply { start() }.looper)
        imageReader.setOnImageAvailableListener({ reader ->
            reader?.acquireNextImage()?.close()
            frameCount++
        }, handler)

        // デコード
        var currentMs = 0L
        while (true) {
            val seekResult = videoDecoder.seekTo(currentMs)
            currentMs += TEST_VIDEO_FRAME_MS
            if (!seekResult.isSuccessful) break
        }

        // 少なくとも フレームレートx秒 の数だけ映像フレームがでていること
        assertTrue { CommonTestTool.TEST_VIDEO_FPS * durationSec < frameCount }
    }

    @Test
    fun test_長い動画でシークが遠い場合は先にキーフレームまでシークする機能が使えること() = runTest(timeout = (CommonTestTool.DEFAULT_DISPATCH_TIMEOUT_MS * 10).milliseconds) {
        // テスト動画作成、長めにして確認
        // キーフレーム間隔は1秒
        val simpleDateFormat = SimpleDateFormat("MM:ss")
        val paint = Paint().apply {
            color = Color.RED
            textSize = 50f
        }
        val durationSec = 60
        val testFile = CommonTestTool.createTestVideo(
            durationMs = durationSec * 1_000L,
            onDrawRequest = {
                drawCanvas {
                    drawText(simpleDateFormat.format(it), 100f, 100f, paint)
                }
            }
        )

        // デコーダー
        val imageReader = ImageReader.newInstance(CommonTestTool.TEST_VIDEO_WIDTH, CommonTestTool.TEST_VIDEO_HEIGHT, ImageFormat.PRIVATE, 32)
        val videoDecoder = AkariVideoDecoder().apply {
            prepare(
                input = testFile.toAkariCoreInputOutputData(),
                outputSurface = imageReader.surface
            )
        }

        val frameTimestampMsList = mutableListOf<Long>()
        val handler = Handler(HandlerThread("HandlerThreadDispatcher").apply { start() }.looper)
        imageReader.setOnImageAvailableListener({ render ->
            render.acquireNextImage().apply {
                frameTimestampMsList += this.timestamp.nanoseconds.inWholeMilliseconds
            }.close()
        }, handler)


        // 1秒間隔でキーフレームが存在するので、1秒あたりでシーク判定され、4秒くらいまでシークされるはず
        // よって、1..4 秒のフレームはシークで存在しないはず
        videoDecoder.seekTo(5_000)
        assertTrue { frameTimestampMsList.toList().all { it !in 1_000..4_000 } }

        // 同様に5秒から55秒にシークしても、それまでのフレームはないはず
        videoDecoder.seekTo(55_000)
        assertTrue { frameTimestampMsList.toList().all { it !in 6_000..54_000 } }
    }

}