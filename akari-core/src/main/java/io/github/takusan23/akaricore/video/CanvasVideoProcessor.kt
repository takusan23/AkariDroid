package io.github.takusan23.akaricore.video

import android.graphics.Canvas
import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoEncoder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Canvas から動画を作る */
object CanvasVideoProcessor {

    /**
     * 処理を開始する
     *
     * @param output 出力先ファイル。全てのバージョンで動くのは[AkariCoreInputOutput.JavaFile]のみです。
     * @param codecName コーデック名
     * @param containerFormat コンテナフォーマット
     * @param bitRate ビットレート
     * @param frameRate フレームレート
     * @param keyframeInterval キーフレームの間隔
     * @param outputVideoWidth 動画の高さ
     * @param outputVideoHeight 動画の幅
     * @param onCanvasDrawRequest Canvasの描画が必要になったら呼び出される。1フレームごとに呼ばれます（60fpsの場合は60回呼ばれる）。trueを返している間、動画を作成する
     */
    suspend fun start(
        output: AkariCoreInputOutput.Output,
        bitRate: Int = 1_000_000,
        frameRate: Int = 30,
        keyframeInterval: Int = 1,
        outputVideoWidth: Int = 1280,
        outputVideoHeight: Int = 720,
        codecName: String = MediaFormat.MIMETYPE_VIDEO_AVC,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        onCanvasDrawRequest: suspend Canvas.(positionMs: Long) -> Boolean,
    ) {
        // MediaCodec エンコーダー
        val akariVideoEncoder = AkariVideoEncoder().apply {
            prepare(
                output = output,
                bitRate = bitRate,
                frameRate = frameRate,
                keyframeInterval = keyframeInterval,
                outputVideoWidth = outputVideoWidth,
                outputVideoHeight = outputVideoHeight,
                codecName = codecName,
                containerFormat = containerFormat,
                tenBitHdrParametersOrNullSdr = null
            )
        }

        // OpenGL ES で描画するやつ、Canvas で書いて転写する
        val akariGraphicsProcessor = AkariGraphicsProcessor(
            outputSurface = akariVideoEncoder.getInputSurface(),
            width = outputVideoWidth,
            height = outputVideoHeight,
            isEnableTenBitHdr = false
        ).apply { prepare() }

        try {
            coroutineScope {
                val encoderJob = launch {
                    akariVideoEncoder.start()
                }
                val graphicsJob = launch {
                    // 1フレームの時間
                    // 60fps なら 16ms、30fps なら 33ms
                    val frameMs = 1_000 / frameRate
                    // 経過時間
                    var currentPositionMs = 0L
                    val loopContinueData = AkariGraphicsProcessor.LoopContinueData(isRequestNextFrame = true, currentFrameNanoSeconds = 0)
                    akariGraphicsProcessor.drawLoop {

                        drawCanvas {
                            loopContinueData.isRequestNextFrame = onCanvasDrawRequest(this, currentPositionMs)
                        }
                        loopContinueData.currentFrameNanoSeconds = currentPositionMs * AkariGraphicsProcessor.LoopContinueData.MILLI_SECONDS_TO_NANO_SECONDS

                        currentPositionMs += frameMs
                        loopContinueData
                    }
                }
                graphicsJob.join()
                encoderJob.cancel()
            }
        } finally {
            akariGraphicsProcessor.destroy()
        }
    }
}