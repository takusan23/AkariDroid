package io.github.takusan23.akaricore.v1.processor

import android.graphics.Canvas
import android.media.*
import io.github.takusan23.akaricore.v1.gl.MediaCodecInputSurface
import io.github.takusan23.akaricore.v1.gl.TextureRenderer
import io.github.takusan23.akaricore.v1.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/** OpenGLを利用して動画にCanvasを重ねる */
@Deprecated(message = "v2")
object VideoCanvasProcessor {

    /** タイムアウト */
    private const val TIMEOUT_US = 10000L

    /** MediaCodecでもらえるInputBufferのサイズ */
    private const val INPUT_BUFFER_SIZE = 655360

    /** トラック番号が空の場合 */
    private const val UNDEFINED_TRACK_INDEX = -1

    /**
     * [MediaFormat.KEY_ROTATION]
     * MediaFormat.KEY_ROTATION の定数。Android 6 以上だがフラグ自体は 5 から存在するらしいので
     */
    private const val KEY_ROTATION = "rotation-degrees"

    /**
     * 処理を始める、終わるまで一時停止します
     *
     * @param videoFile フィルターをかけたい動画ファイル
     * @param resultFile 出力ファイル
     * @param bitRate ビットレート
     * @param frameRate フレームレート
     * @param videoCodec エンコード後の動画コーデック [MediaFormat.MIMETYPE_VIDEO_AVC] など
     * @param containerFormat コンテナフォーマット [MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4] など
     * @param outputVideoWidth 動画の高さを変える場合は変えられます。16の倍数であることが必須です
     * @param outputVideoHeight 動画の幅を変える場合は変えられます。16の倍数であることが必須です
     * @param onCanvasDrawRequest Canvasへ描画リクエストが来た際に呼ばれる。Canvasと再生時間（ミリ秒）が渡されます
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun start(
        videoFile: File,
        resultFile: File,
        videoCodec: String = MediaFormat.MIMETYPE_VIDEO_AVC,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        bitRate: Int = 1_000_000,
        frameRate: Int = 30,
        outputVideoWidth: Int = 1280,
        outputVideoHeight: Int = 720,
        onCanvasDrawRequest: Canvas.(positionMs: Long) -> Unit,
    ) = withContext(Dispatchers.Default) {

        // 動画の情報を読み出す
        val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(videoFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO) ?: return@withContext
        // トラックを選択
        mediaExtractor.selectTrack(index)
        mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        // 解析結果から各パラメータを取り出す
        val videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
        val videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
        // 画面回転情報
        val hasRotation = format.getIntegerOrNull(KEY_ROTATION) == 90
        // 画面回転度がある場合は width / height がそれぞれ入れ替わるので注意（一敗）
        val originVideoWidth = if (hasRotation) videoHeight else videoWidth
        val originVideoHeight = if (hasRotation) videoWidth else videoHeight

        // エンコード用（生データ -> H.264）MediaCodec
        val encodeMediaCodec = MediaCodec.createEncoderByType(videoCodec).apply {
            // エンコーダーにセットするMediaFormat
            // コーデックが指定されていればそっちを使う
            val videoMediaFormat = MediaFormat.createVideoFormat(videoCodec, outputVideoWidth, outputVideoHeight).apply {
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, INPUT_BUFFER_SIZE)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            configure(videoMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }

        // エンコーダーのSurfaceを取得して、OpenGLを利用してCanvasを重ねます
        val mediaCodecInputSurface = MediaCodecInputSurface(
            encodeMediaCodec.createInputSurface(),
            TextureRenderer(
                outputVideoWidth = outputVideoWidth,
                outputVideoHeight = outputVideoHeight,
                originVideoWidth = originVideoWidth,
                originVideoHeight = originVideoHeight,
                videoRotation = if (hasRotation) 270f else 0f
            )
        )
        mediaCodecInputSurface.makeCurrent()
        encodeMediaCodec.start()

        // デコード用（H.264 -> 生データ）MediaCodec
        mediaCodecInputSurface.createRender()
        val decodeMediaCodec = MediaCodec.createDecoderByType(videoCodec).apply {
            // 画面回転データが有った場合にリセットする
            // このままだと回転されたままなので、OpenGL 側で回転させる
            // setInteger をここでやるのは良くない気がするけど面倒なので
            format.setInteger(KEY_ROTATION, 0)
            configure(format, mediaCodecInputSurface.drawSurface, null, 0)
        }
        decodeMediaCodec.start()

        // 保存
        var videoTrackIndex = UNDEFINED_TRACK_INDEX
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat ?: MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // メタデータ格納用
        val bufferInfo = MediaCodec.BufferInfo()
        var outputDone = false
        var inputDone = false

        while (!outputDone) {

            // コルーチンキャンセル時は強制終了
            if (!isActive) break

            if (!inputDone) {
                val inputBufferId = decodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferId >= 0) {
                    val inputBuffer = decodeMediaCodec.getInputBuffer(inputBufferId)!!
                    val size = mediaExtractor.readSampleData(inputBuffer, 0)
                    if (size > 0) {
                        // デコーダーへ流す
                        // 今までの動画の分の再生位置を足しておく
                        decodeMediaCodec.queueInputBuffer(inputBufferId, 0, size, mediaExtractor.sampleTime, 0)
                        mediaExtractor.advance()
                    } else {
                        // 終了
                        decodeMediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        // 開放
                        mediaExtractor.release()
                        // 終了
                        inputDone = true
                    }
                }
            }

            var decoderOutputAvailable = true
            while (decoderOutputAvailable) {
                // Surface経由でデータを貰って保存する
                val encoderStatus = encodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (encoderStatus >= 0) {
                    val encodedData = encodeMediaCodec.getOutputBuffer(encoderStatus)!!
                    if (bufferInfo.size > 1) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            // MediaMuxer へ addTrack した後
                            mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    outputDone = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
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
                // Surfaceへレンダリングする。そしてOpenGLでゴニョゴニョする
                val outputBufferId = decodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    decoderOutputAvailable = false
                } else if (outputBufferId >= 0) {
                    // 進捗
                    val doRender = bufferInfo.size != 0
                    decodeMediaCodec.releaseOutputBuffer(outputBufferId, doRender)
                    if (doRender) {
                        var errorWait = false
                        try {
                            mediaCodecInputSurface.awaitNewImage()
                        } catch (e: Exception) {
                            errorWait = true
                        }
                        if (!errorWait) {
                            // 映像とCanvasを合成する
                            mediaCodecInputSurface.drawImage { canvas ->
                                onCanvasDrawRequest(canvas, bufferInfo.presentationTimeUs / 1000L)
                            }
                            mediaCodecInputSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000)
                            mediaCodecInputSurface.swapBuffers()
                        }
                    }
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        decoderOutputAvailable = false
                        encodeMediaCodec.signalEndOfInputStream()
                    }
                }
            }
        }

        // デコーダー終了
        decodeMediaCodec.stop()
        decodeMediaCodec.release()
        // OpenGL開放
        mediaCodecInputSurface.release()
        // エンコーダー終了
        encodeMediaCodec.stop()
        encodeMediaCodec.release()
        // MediaMuxerも終了
        mediaMuxer.stop()
        mediaMuxer.release()
    }

    /**
     * [MediaFormat.getInteger]、キーがなければ null を返す
     *
     * @param name [MediaFormat.KEY_ROTATION]など
     */
    private fun MediaFormat.getIntegerOrNull(name: String): Int? {
        return if (containsKey(name)) {
            getInteger(name)
        } else null
    }
}