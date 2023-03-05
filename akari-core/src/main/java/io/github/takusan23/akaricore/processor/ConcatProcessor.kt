package io.github.takusan23.akaricore.processor

import android.media.*
import io.github.takusan23.akaricore.common.AudioDecoder
import io.github.takusan23.akaricore.common.AudioEncoder
import io.github.takusan23.akaricore.gl.MediaCodecInputSurface
import io.github.takusan23.akaricore.gl.TextureRenderer
import io.github.takusan23.akaricore.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/** 複数の動画を連結する */
object ConcatProcessor {

    /** 一時ファイルの名前 */
    private const val TEMP_RAW_AUDIO_FILE_NAME = "temp_raw_audio"

    /** タイムアウト */
    private const val TIMEOUT_US = 10000L

    /** トラック番号が空の場合 */
    private const val UNDEFINED_TRACK_INDEX = -1

    /**
     * 映像の結合を行う
     *
     * @param videoFileList 結合する動画ファイル
     * @param resultFile 出力ファイル
     * @param videoCodec 動画コーデック
     * @param containerFormat コンテナフォーマット
     * @param videoBitRate ビットレート
     * @param frameRate フレームレート
     * @param outputVideoWidth 出力動画の幅
     * @param outputVideoHeight 出力動画の高さ
     */
    suspend fun concatVideo(
        videoFileList: List<File>,
        resultFile: File,
        videoCodec: String = MediaFormat.MIMETYPE_VIDEO_AVC,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        videoBitRate: Int = 1_000_000,
        frameRate: Int = 30,
        outputVideoWidth: Int = 1280,
        outputVideoHeight: Int = 720,
    ) = withContext(Dispatchers.Default) {
        val videoFileIterator = videoFileList.iterator()
        var videoTrackIndex = UNDEFINED_TRACK_INDEX
        var currentMediaExtractor: MediaExtractor? = null
        var currentMediaFormat: MediaFormat? = null

        // currentMediaExtractor / currentMediaFormat を更新する
        suspend fun extract(videoFile: File) {
            // 動画の情報を読み出す
            val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(videoFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_VIDEO)!!
            currentMediaExtractor = mediaExtractor
            currentMediaFormat = format
            // トラックを選択
            mediaExtractor.selectTrack(index)
            mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        }

        // 最初の動画を MediaExtractor で取り出す
        extract(videoFileIterator.next())

        // エンコード用（生データ -> H.264）MediaCodec
        val encodeMediaCodec = MediaCodec.createEncoderByType(videoCodec).apply {
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
            configure(currentMediaFormat!!, mediaCodecInputSurface.drawSurface, null, 0)
        }
        decodeMediaCodec.start()

        // 保存先
        // MediaMuxer でコンテナに格納する
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)

        // 前回の動画ファイルを足した動画時間
        var totalPresentationTime = 0L
        var prevPresentationTime = 0L

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
                    val size = currentMediaExtractor!!.readSampleData(inputBuffer, 0)
                    if (size > 0) {
                        // デコーダーへ流す
                        // 今までの動画の分の再生位置を足しておく
                        decodeMediaCodec.queueInputBuffer(inputBufferId, 0, size, currentMediaExtractor!!.sampleTime + totalPresentationTime, 0)
                        currentMediaExtractor!!.advance()
                        // 一個前の動画の動画サイズを控えておく
                        // else で extractor.sampleTime すると既に-1にっているので
                        if (currentMediaExtractor!!.sampleTime != -1L) {
                            prevPresentationTime = currentMediaExtractor!!.sampleTime
                        }
                    } else {
                        totalPresentationTime += prevPresentationTime
                        // データがないので次データへ
                        if (videoFileIterator.hasNext()) {
                            // 多分いる
                            decodeMediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0, 0)
                            // 動画の情報を読み出す
                            currentMediaExtractor!!.release()
                            // 次のデータへ
                            extract(videoFileIterator.next())
                        } else {
                            // データなくなった場合は終了
                            decodeMediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            // 開放
                            currentMediaExtractor!!.release()
                            // 終了
                            inputDone = true
                        }
                    }
                }
            }

            var decoderOutputAvailable = true
            while (decoderOutputAvailable) {

                // コルーチンキャンセル時は強制終了
                if (!isActive) break

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
                    if (videoTrackIndex == UNDEFINED_TRACK_INDEX) {
                        // MediaMuxerへ映像トラックを追加するのはこのタイミングで行う
                        // このタイミングでやると固有のパラメーターがセットされたMediaFormatが手に入る(csd-0 とか)
                        // 映像がぶっ壊れている場合（緑で塗りつぶされてるとか）は多分このあたりが怪しい
                        val newFormat = encodeMediaCodec.outputFormat
                        videoTrackIndex = mediaMuxer.addTrack(newFormat)
                        mediaMuxer.start()
                    }
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
                            mediaCodecInputSurface.drawImage()
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
    suspend fun concatAudio(
        videoFileList: List<File>,
        tempFolder: File,
        resultFile: File,
        audioCodec: String = MediaFormat.MIMETYPE_AUDIO_AAC,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        audioBitRate: Int = 128_000,
        samplingRate: Int = 44_100,
    ) = withContext(Dispatchers.Default) {
        // エンコーダー起動
        val encodeMediaCodec = MediaCodec.createEncoderByType(audioCodec).apply {
            val encodeMediaFormat = MediaFormat.createAudioFormat(audioCodec, samplingRate, 2).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate)
            }
            configure(encodeMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
        encodeMediaCodec.start()

        // 保存先
        // MediaMuxer でコンテナに格納する
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)

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
            mediaExtractor.release()
            audioDecoder.release()
        }
        // 終わったらエンコードする
        var audioTrackIndex = UNDEFINED_TRACK_INDEX
        val audioEncoder = AudioEncoder()
        val tempRawFileInputStream = tempRawFile.inputStream()
        audioEncoder.prepareEncoder(
            codec = audioCodec,
            sampleRate = samplingRate,
            channelCount = 2,
            bitRate = audioBitRate,
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
    }
}