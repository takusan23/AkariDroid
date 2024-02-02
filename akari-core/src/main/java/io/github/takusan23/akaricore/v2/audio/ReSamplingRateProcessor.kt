package io.github.takusan23.akaricore.v2.audio

import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.akaricore.v1.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** [Sonic]を利用した、サンプリングレート変換器 */
object ReSamplingRateProcessor {

    private const val UNDEFINED_TRACK_INDEX = -1

    /** デコードした音声のファイル */
    private const val TEMP_FILE_NAME_DECODE_RAW = "raw_audio_file"

    /** サンプリングレート変換後のファイル */
    private const val TEMP_FILE_NAME_RE_SAMPLING = "resampling_audio_file"

    /**
     * サンプリングレート変換をする
     *
     * @param audioFile 音声ファイル
     * @param resultFile 変換したファイル
     * @param tempFolder 一時ファイル置き場
     * @param codecName コーデック名
     * @param containerFormat コンテナフォーマット
     * @param resultSamplingRate 変換したいサンプリングレート。48_000 とか。
     */
    suspend fun start(
        audioFile: File,
        resultFile: File,
        tempFolder: File,
        resultSamplingRate: Int,
        codecName: String = MediaFormat.MIMETYPE_AUDIO_AAC,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    ) = withContext(Dispatchers.Default) {
        // 仮の保存先を作成
        val rawAudioFile = tempFolder.resolve(TEMP_FILE_NAME_DECODE_RAW)
        val reSamplingRateAudioFile = tempFolder.resolve(TEMP_FILE_NAME_RE_SAMPLING)

        try {
            // デコードする
            val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(audioFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO)!!
            mediaExtractor.selectTrack(index)

            // デコーダー起動
            val audioDecoder = AudioDecoder().apply {
                prepareDecoder(format)
            }
            var decoderMediaFormat: MediaFormat? = null
            rawAudioFile.outputStream().use { outputStream ->
                audioDecoder.startAudioDecode(
                    readSampleData = { byteBuffer ->
                        val size = mediaExtractor.readSampleData(byteBuffer, 0)
                        mediaExtractor.advance()
                        return@startAudioDecode size to mediaExtractor.sampleTime
                    },
                    onOutputBufferAvailable = { bytes ->
                        outputStream.write(bytes)
                    },
                    onOutputFormat = { mediaFormat ->
                        decoderMediaFormat = mediaFormat
                    }
                )
            }
            mediaExtractor.release()

            // サンプリングレート変換を行う
            val channelCount = decoderMediaFormat!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val inSamplingRate = decoderMediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            reSamplingBySonic(
                inPcmFile = rawAudioFile,
                outPcmFile = reSamplingRateAudioFile,
                channelCount = channelCount,
                inSamplingRate = inSamplingRate,
                outSamplingRate = resultSamplingRate
            )

            // エンコードする
            // コンテナフォーマット
            val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)
            var trackIndex = UNDEFINED_TRACK_INDEX
            // エンコーダー起動
            val audioEncoder = AudioEncoder().apply {
                prepareEncoder(
                    codec = codecName,
                    sampleRate = resultSamplingRate,
                    channelCount = channelCount
                )
            }
            reSamplingRateAudioFile.inputStream().use { inputStream ->
                audioEncoder.startAudioEncode(
                    onRecordInput = { byteArray -> inputStream.read(byteArray) },
                    onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                        if (trackIndex != -1) {
                            mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo)
                        }
                    },
                    onOutputFormatAvailable = {
                        trackIndex = mediaMuxer.addTrack(it)
                        mediaMuxer.start()
                    }
                )
            }
            mediaMuxer.stop()
            mediaMuxer.release()
        } finally {
            reSamplingRateAudioFile.delete()
            rawAudioFile.delete()
        }
    }

    /**
     * Sonic ライブラリを使ってアップサンプリングする
     *
     * @param inPcmFile 変換前 PCM ファイル
     * @param outPcmFile 変換後 PCM ファイル
     * @param channelCount チャンネル数
     * @param inSamplingRate 変換前のサンプリングレート
     * @param outSamplingRate 変換後のサンプリングレート
     */
    private suspend fun reSamplingBySonic(
        inPcmFile: File,
        outPcmFile: File,
        channelCount: Int,
        inSamplingRate: Int,
        outSamplingRate: Int
    ) = withContext(Dispatchers.Default) {
        val bufferSize = 8192
        val inByteArray = ByteArray(bufferSize)
        val outByteArray = ByteArray(bufferSize)
        var numRead: Int
        var numWritten: Int

        // Sonic を利用してアップサンプリングを行う
        val sonic = Sonic(inSamplingRate, channelCount)
        sonic.sampleRate = outSamplingRate
        sonic.speed = 1f
        sonic.pitch = 1f
        sonic.rate = inSamplingRate.toFloat() / outSamplingRate.toFloat()
        sonic.volume = 1f
        sonic.chordPitch = false
        sonic.quality = 0

        inPcmFile.inputStream().use { inputStream ->
            outPcmFile.outputStream().use { outputStream ->

                do {
                    numRead = inputStream.read(inByteArray, 0, bufferSize)
                    if (numRead <= 0) {
                        sonic.flushStream()
                    } else {
                        sonic.writeBytesToStream(inByteArray, numRead)
                    }
                    do {
                        numWritten = sonic.readBytesFromStream(outByteArray, bufferSize)
                        if (numWritten > 0) {
                            outputStream.write(outByteArray, 0, numWritten)
                        }
                    } while (numWritten > 0)
                } while (numRead > 0)
            }
        }
    }

}