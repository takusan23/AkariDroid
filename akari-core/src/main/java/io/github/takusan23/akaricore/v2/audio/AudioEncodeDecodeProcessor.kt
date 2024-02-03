package io.github.takusan23.akaricore.v2.audio

import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.akaricore.v1.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AAC 等のエンコードされた音声ファイルを PCM に、
 * また、PCM を AAC 等のエンコードされたデータに
 */
object AudioEncodeDecodeProcessor {

    private const val UNDEFINED_TRACK_INDEX = -1

    /**
     * デコードする
     *
     * @param inAudioFile AAC ファイル入力
     * @param outPcmFile PCM ファイル出力
     * @param onOutputFormat デコード時にでてくる [MediaFormat]
     */
    suspend fun decode(
        inAudioFile: File,
        outPcmFile: File,
        onOutputFormat: (MediaFormat) -> Unit = {}
    ) = withContext(Dispatchers.Default) {
        // デコードする
        val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(inAudioFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO)!!
        mediaExtractor.selectTrack(index)

        // デコーダー起動
        val audioDecoder = AudioDecoder().apply {
            prepareDecoder(format)
        }
        outPcmFile.outputStream().use { outputStream ->
            audioDecoder.startAudioDecode(
                readSampleData = { byteBuffer ->
                    val size = mediaExtractor.readSampleData(byteBuffer, 0)
                    mediaExtractor.advance()
                    return@startAudioDecode size to mediaExtractor.sampleTime
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
     * @param inPcmFile PCM ファイル入力
     * @param outAudioFile AAC ファイル出力
     * @param codecName コーデック
     * @param containerFormat コンテナフォーマット
     * @param samplingRate サンプリングレート
     * @param bitRate ビットレート
     */
    suspend fun encode(
        inPcmFile: File,
        outAudioFile: File,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        codecName: String = MediaFormat.MIMETYPE_AUDIO_AAC,
        samplingRate: Int = 44_100,
        bitRate: Int = 192_000,
        channelCount: Int = 2
    ) = withContext(Dispatchers.Default) {
        // エンコードする
        // コンテナフォーマット
        val mediaMuxer = MediaMuxer(outAudioFile.path, containerFormat)
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
        inPcmFile.inputStream().use { inputStream ->
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