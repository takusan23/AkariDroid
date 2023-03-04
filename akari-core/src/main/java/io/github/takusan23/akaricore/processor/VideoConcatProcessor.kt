package io.github.takusan23.akaricore.processor

import android.media.*
import io.github.takusan23.akaricore.common.AudioDecoder
import io.github.takusan23.akaricore.common.AudioEncoder
import io.github.takusan23.akaricore.gl.MediaCodecInputSurface
import io.github.takusan23.akaricore.gl.TextureRenderer
import io.github.takusan23.akaricore.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 複数の動画を連結する
 *
 *
 */
class VideoConcatProcessor(
    private val videoFileList: List<File>,
    private val tempFolder: File,
    private val resultFile: File,
    private val videoCodec: String = MediaFormat.MIMETYPE_VIDEO_AVC,
    private val audioCodec: String = MediaFormat.MIMETYPE_AUDIO_AAC,
    private val containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
    private val videoBitRate: Int = 1_000_000,
    private val frameRate: Int = 30,
    private val outputVideoWidth: Int = 1280,
    private val outputVideoHeight: Int = 720,
    private val audioBitRate: Int = 128_000,
    private val samplingRate: Int = 44_100,
) {

    /** 処理を開始する */
    suspend fun start() = withContext(Dispatchers.Default) {
        val videoFile = async { concatVideo() }.await()
        //val audioFile = async { concatAudio() }
        // val videoAndAudioFile = listOf(videoFile, audioFile).awaitAll()
        // MediaMuxerTool.mixed(
        //     resultFile = resultFile,
        //     containerFormat = containerFormat,
        //     mergeFileList = videoAndAudioFile
        // )
        // withContext(Dispatchers.IO) {
        //     tempFolder.deleteRecursively()
        // }
    }

    /** 映像の結合を行う */
    private suspend fun concatVideo() = withContext(Dispatchers.Default) {

        // 動画の情報を読み出す
        val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(videoFileList.first().path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO) ?: return@withContext
        // トラックを選択
        mediaExtractor.selectTrack(index)
        mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        var videoTrackIndex = UNDEFINED_TRACK_INDEX

        // エンコード用（生データ -> H.264）MediaCodec
        val encodeMediaCodec = MediaCodec.createEncoderByType(videoCodec).apply {
            // エンコーダーにセットするMediaFormat
            // コーデックが指定されていればそっちを使う
            val videoMediaFormat = MediaFormat.createVideoFormat(videoCodec, outputVideoWidth, outputVideoHeight).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, videoBitRate)
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
                originVideoWidth = outputVideoWidth,
                originVideoHeight = outputVideoHeight,
                videoRotation = 0f
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
            format.setInteger(MediaFormat.KEY_ROTATION, 0)
            configure(format, mediaCodecInputSurface!!.drawSurface, null, 0)
        }
        decodeMediaCodec.start()

        val tempAudioFile = tempFolder.resolve(TEMP_CONCAT_VIDEO_FILE_NAME).apply { createNewFile() }
        val mediaMuxer = MediaMuxer(tempAudioFile.path, containerFormat)

        // メタデータ格納用
        val bufferInfo = MediaCodec.BufferInfo()
        var outputDone = false
        var inputDone = false

        while (!outputDone) {
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
                            mediaCodecInputSurface.drawImage { }
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

    /** 音声の結合を行う */
    private suspend fun concatAudio() = withContext(Dispatchers.Default) {
        // エンコーダー起動
        val encodeMediaCodec = MediaCodec.createEncoderByType(audioCodec).apply {
            val encodeMediaFormat = MediaFormat.createAudioFormat(audioCodec, samplingRate, 2).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate)
            }
            configure(encodeMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
        encodeMediaCodec.start()

        // 一時ファイルにする
        // 最後に音声とミックスするので
        val tempAudioFile = tempFolder.resolve(TEMP_CONCAT_AUDIO_FILE_NAME).apply { createNewFile() }
        val mediaMuxer = MediaMuxer(tempAudioFile.path, containerFormat)

        // PCMデータを入れておくファイルパス
        val tempRawFile = tempFolder.resolve(TEMP_RAW_AUDIO_FILE_NAME).apply { createNewFile() }
        videoFileList.map { audioFile ->
            val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(audioFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO)!!
            mediaExtractor.selectTrack(index)
            // デコードする
            val audioDecoder = AudioDecoder()
            audioDecoder.prepareDecoder(format)
            audioDecoder.startAudioDecode(
                readSampleData = { byteBuffer ->
                    val size = mediaExtractor.readSampleData(byteBuffer, 0)
                    mediaExtractor.advance()
                    // 動画時間の方がまだ長い場合は継続。動画のほうが短くても終わる
                    return@startAudioDecode size to mediaExtractor.sampleTime
                },
                onOutputBufferAvailable = { bytes -> tempRawFile.appendBytes(bytes) }
            )
            audioDecoder.release()
        }
        // 終わったらエンコードする
        var audioTrackIndex = UNDEFINED_TRACK_INDEX
        val audioEncoder = AudioEncoder()
        val tempRawFileInputStream = tempRawFile.inputStream()
        audioEncoder.prepareEncoder(
            sampleRate = samplingRate,
            channelCount = 2,
            bitRate = audioBitRate,
            isOpus = false
        )
        audioEncoder.startAudioEncode(
            onRecordInput = { byteArray -> tempRawFileInputStream.read(byteArray) },
            onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                if (audioTrackIndex != UNDEFINED_TRACK_INDEX) {
                    mediaMuxer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
                }
            },
            onOutputFormatAvailable = {
                audioTrackIndex = mediaMuxer.addTrack(it)
                mediaMuxer.start()
            },
        )
        tempRawFileInputStream.close()
        mediaMuxer.stop()
        mediaMuxer.release()
        tempRawFile.delete()
        audioEncoder.release()
        return@withContext tempAudioFile
    }

    companion object {
        private const val TEMP_CONCAT_VIDEO_FILE_NAME = "temp_concat_video"
        private const val TEMP_CONCAT_AUDIO_FILE_NAME = "temp_concat_audio"
        private const val TEMP_RAW_AUDIO_FILE_NAME = "temp_raw_audio"
        private const val TIMEOUT_US = 10000L
        private const val UNDEFINED_TRACK_INDEX = -1
    }

}