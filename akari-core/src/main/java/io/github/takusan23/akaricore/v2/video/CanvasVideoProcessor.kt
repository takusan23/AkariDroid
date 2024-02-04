package io.github.takusan23.akaricore.v2.video

import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.akaricore.v2.video.gl.InputSurface
import io.github.takusan23.akaricore.v2.video.gl.TextureRenderer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.io.File

/** Canvas から動画を作る */
object CanvasVideoProcessor {

    /** タイムアウト */
    private const val TIMEOUT_US = 10000L

    /** OpenGL 描画用スレッドの Kotlin Coroutine Dispatcher */
    @OptIn(DelicateCoroutinesApi::class)
    private val openGlRelatedThreadDispatcher = newSingleThreadContext("openGlRelatedThreadDispatcher")

    /**
     * 処理を開始する
     *
     * @param resultFile 出力先
     * @param codecName コーデック名
     * @param containerFormat コンテナフォーマット
     * @param bitRate ビットレート
     * @param frameRate フレームレート
     * @param outputVideoWidth 動画の高さ
     * @param outputVideoHeight 動画の幅
     * @param onCanvasDrawRequest Canvasの描画が必要になったら呼び出される。1フレームごとに呼ばれます（60fpsの場合は60回呼ばれる）。trueを返している間、動画を作成する
     */
    suspend fun start(
        resultFile: File,
        bitRate: Int = 1_000_000,
        frameRate: Int = 30,
        outputVideoWidth: Int = 1280,
        outputVideoHeight: Int = 720,
        codecName: String = MediaFormat.MIMETYPE_VIDEO_AVC,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        onCanvasDrawRequest: suspend Canvas.(positionMs: Long) -> Boolean,
    ) = withContext(Dispatchers.Default) {
        val encodeMediaCodec = MediaCodec.createEncoderByType(codecName).apply {
            // エンコーダーにセットするMediaFormat
            // コーデックが指定されていればそっちを使う
            val videoMediaFormat = MediaFormat.createVideoFormat(codecName, outputVideoWidth, outputVideoHeight).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            configure(videoMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }

        // OpenGL の初期化をする。OpenGL 関連の関数を呼び出す場合は、OpenGL 用に用意したスレッドに切り替えてから
        val canvasInputSurface = withContext(openGlRelatedThreadDispatcher) {
            // エンコーダーのSurfaceを取得して、OpenGLを利用してCanvasを重ねます
            InputSurface(
                encodeMediaCodec.createInputSurface(),
                TextureRenderer(
                    outputVideoWidth = outputVideoWidth,
                    outputVideoHeight = outputVideoHeight
                )
            ).apply {
                makeCurrent()
                // エンコーダー開始
                encodeMediaCodec.start()
                createRender()
            }
        }

        // マルチプレクサ
        var videoTrackIndex = -1
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)

        // 終了フラグ
        var outputDone = false

        // OpenGL の描画用メインループ。
        // 先述の通り、OpenGL はスレッドでコンテキストを識別するため、OpenGL 用スレッドに切り替える必要あり。
        val openGlRenderingJob = launch(openGlRelatedThreadDispatcher) {
            // 1フレームの時間
            // 60fps なら 16ms、30fps なら 33ms
            val frameMs = 1_000 / frameRate
            // 経過時間。マイクロ秒
            var currentPositionUs = 0L
            try {
                while (!outputDone) {
                    // コルーチンキャンセル時は強制終了
                    if (!isActive) break

                    // OpenGL で描画する
                    // Canvas の入力をする
                    var isRunning = false
                    canvasInputSurface.drawCanvas { canvas ->
                        isRunning = onCanvasDrawRequest(canvas, currentPositionUs / 1_000L)
                    }
                    canvasInputSurface.setPresentationTime(currentPositionUs * 1_000L)
                    canvasInputSurface.swapBuffers()
                    if (!isRunning) {
                        outputDone = true
                        encodeMediaCodec.signalEndOfInputStream()
                    }
                    // 時間を増やす
                    // 1 フレーム分の時間。ミリ秒なので増やす
                    currentPositionUs += frameMs * 1_000L
                }
            } finally {
                // リソース開放
                canvasInputSurface.release()
            }
        }

        // エンコーダーのループ
        val encoderJob = launch {
            // メタデータ
            val bufferInfo = MediaCodec.BufferInfo()
            try {
                while (!outputDone) {
                    // コルーチンキャンセル時は強制終了
                    if (!isActive) break

                    // Surface経由でデータを貰って保存する
                    val encoderStatus = encodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    if (encoderStatus >= 0) {
                        if (bufferInfo.size > 0) {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                // MediaMuxer へ addTrack した後
                                val encodedData = encodeMediaCodec.getOutputBuffer(encoderStatus)!!
                                mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            }
                        }
                        encodeMediaCodec.releaseOutputBuffer(encoderStatus, false)
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // MediaMuxerへ映像トラックを追加するのはこのタイミングで行う
                        // このタイミングでやると固有のパラメーターがセットされたMediaFormatが手に入る(csd-0 とか)
                        // 映像がぶっ壊れている場合（緑で塗りつぶされてるとか）は多分このあたりが怪しい
                        val newFormat = encodeMediaCodec.outputFormat
                        videoTrackIndex = mediaMuxer.addTrack(newFormat)
                        mediaMuxer.start()
                    }
                    if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        continue
                    }
                }
            } finally {
                // エンコーダー終了
                encodeMediaCodec.stop()
                encodeMediaCodec.release()
                // MediaMuxerも終了
                mediaMuxer.stop()
                mediaMuxer.release()
            }
        }

        // それぞれのメインループが終わるまで、コルーチンを一時停止
        openGlRenderingJob.join()
        encoderJob.join()
    }
}