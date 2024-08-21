package io.github.takusan23.akaricore.audio

import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.common.MediaExtractorTool
import io.github.takusan23.akaricore.common.MediaMuxerTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AAC 等のエンコードされた音声ファイルを PCM に、
 * また、PCM を AAC 等のエンコードされたデータに
 */
object AudioEncodeDecodeProcessor {

    private const val UNDEFINED_TRACK_INDEX = -1

    /**
     * デコードする
     *
     * @param inAudioData AAC ファイル入力
     * @param outPcmFile PCM ファイル出力
     * @param onOutputFormat デコード時にでてくる [MediaFormat]
     */
    suspend fun decode(
        input: AkariCoreInputOutput.Input,
        output: AkariCoreInputOutput.Output,
        onOutputFormat: (MediaFormat) -> Unit = {}
    ) = withContext(Dispatchers.Default) {
        // デコードする
        val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(input, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO)!!
        mediaExtractor.selectTrack(index)

        // デコーダー起動
        val audioDecoder = AudioDecoder().apply {
            prepareDecoder(format)
        }
        output.outputStream().buffered().use { outputStream ->
            audioDecoder.startAudioDecode(
                readSampleData = { byteBuffer ->
                    val size = mediaExtractor.readSampleData(byteBuffer, 0)
                    val sampleTime = mediaExtractor.sampleTime
                    mediaExtractor.advance()
                    return@startAudioDecode size to sampleTime
                },
                onOutputBufferAvailable = { bytes ->
                    outputStream.write(bytes)
                },
                onOutputFormat = onOutputFormat
            )
        }
        mediaExtractor.release()
    }

    /**
     * エンコードする
     *
     * @param input PCM ファイル入力
     * @param output AAC ファイル出力
     * @param codecName コーデック
     * @param containerFormat コンテナフォーマット
     * @param samplingRate サンプリングレート
     * @param bitRate ビットレート
     */
    suspend fun encode(
        input: AkariCoreInputOutput.Input,
        output: AkariCoreInputOutput.Output,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        codecName: String = MediaFormat.MIMETYPE_AUDIO_AAC,
        samplingRate: Int = AkariCoreAudioProperties.SAMPLING_RATE,
        bitRate: Int = 192_000,
        channelCount: Int = 2
    ) = withContext(Dispatchers.Default) {
        // エンコードする
        // コンテナフォーマット
        val mediaMuxer = MediaMuxerTool.createMediaMuxer(output, containerFormat)
        var trackIndex = UNDEFINED_TRACK_INDEX
        // エンコーダー起動
        val audioEncoder = AudioEncoder().apply {
            prepareEncoder(
                codec = codecName,
                sampleRate = samplingRate,
                channelCount = channelCount,
                bitRate = bitRate
            )
        }
        input.inputStream().buffered().use { inputStream ->
            audioEncoder.startAudioEncode(
                onRecordInput = { byteArray -> inputStream.read(byteArray) },
                onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                    if (trackIndex != UNDEFINED_TRACK_INDEX) {
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
    }

}