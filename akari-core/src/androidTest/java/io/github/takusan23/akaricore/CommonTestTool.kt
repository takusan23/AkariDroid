package io.github.takusan23.akaricore

import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.test.platform.app.InstrumentationRegistry
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorColorSpaceType
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorRenderingPrepareData
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoEncoder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** インスツルメンツテストで使う関数、定数たち */
object CommonTestTool {

    /** runTest デフォルトタイムアウト */
    const val DEFAULT_DISPATCH_TIMEOUT_MS = 60_000L

    /** 動画の横 */
    const val TEST_VIDEO_WIDTH = 1280

    /** 動画の縦 */
    const val TEST_VIDEO_HEIGHT = 720

    /** 動画のフレームレート */
    const val TEST_VIDEO_FPS = 30

    /** フレームレートをミリ秒で表したもの */
    const val TEST_VIDEO_FRAME_MS = 1000 / TEST_VIDEO_FPS // 30fps は 33ms 毎にフレームがある（1000 / 30 = 33.3..）

    /**
     * インストルメントテストで利用できる保存先を返す
     *
     * @return [File]
     */
    fun getTestExternalFilesDir(): File {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        return appContext.getExternalFilesDir(null)!!
    }

    /**
     * 適当な動画を生成する
     *
     * @param durationMs 動画の時間
     * @param onDrawRequest 動画の毎フレーム呼ばれる
     * @return 動画ファイル
     */
    suspend fun createTestVideo(
        durationMs: Long,
        onDrawRequest: suspend AkariGraphicsTextureRenderer.(currentPositionMs: Long) -> Unit
    ): File {
        val resultFile = getTestExternalFilesDir().resolve("encode_test_${System.currentTimeMillis()}.mp4")
        val akariVideoEncoder = AkariVideoEncoder().apply {
            prepare(
                output = resultFile.toAkariCoreInputOutputData(),
                containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                outputVideoWidth = TEST_VIDEO_WIDTH,
                outputVideoHeight = TEST_VIDEO_HEIGHT,
                frameRate = TEST_VIDEO_FPS,
                bitRate = 1_000_000,
                keyframeInterval = 1,
                codecName = MediaFormat.MIMETYPE_VIDEO_AVC,
                tenBitHdrParametersOrNullSdr = null
            )
        }
        val graphicsProcessor = AkariGraphicsProcessor(
            renderingPrepareData = AkariGraphicsProcessorRenderingPrepareData.SurfaceRendering(
                surface = akariVideoEncoder.getInputSurface(),
                width = TEST_VIDEO_WIDTH,
                height = TEST_VIDEO_HEIGHT
            ),
            colorSpaceType = AkariGraphicsProcessorColorSpaceType.SDR_BT709
        ).apply { prepare() }

        coroutineScope {
            val encoderJob = launch {
                akariVideoEncoder.start()
            }
            val graphicsJob = launch {
                val loopContinueData = AkariGraphicsProcessor.LoopContinueData(isRequestNextFrame = true, currentFrameNanoSeconds = 0)
                var currentMs = 0L
                graphicsProcessor.drawLoop {
                    onDrawRequest(this, currentMs)

                    loopContinueData.isRequestNextFrame = currentMs <= durationMs
                    loopContinueData.currentFrameNanoSeconds = currentMs * AkariGraphicsProcessor.LoopContinueData.MILLI_SECONDS_TO_NANO_SECONDS

                    currentMs += TEST_VIDEO_FRAME_MS
                    loopContinueData
                }
            }

            // graphicsJob が終わったらエンコーダーも止める
            graphicsJob.join()
            encoderJob.cancel()
        }
        graphicsProcessor.destroy()

        return resultFile
    }

}