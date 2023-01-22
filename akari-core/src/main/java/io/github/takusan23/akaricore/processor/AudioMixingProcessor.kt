package io.github.takusan23.akaricore.processor

import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import io.github.takusan23.akaricore.common.AudioDecoder
import io.github.takusan23.akaricore.common.AudioEncoder
import io.github.takusan23.akaricore.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 音声をエンコードする。
 * 複数ファイルを指定すると音声を重ねてエンコードする。
 *
 * 音の合成ですが、難しいことはなく、波の音を重ねることで音がミックスされます。
 * 実装的には、AACをPCMにして（AudacityのRawデータのように）、バイト配列から同じ位置のByteを足すことでできます。
 *
 * @param mixingVolume 動画以外の音声素材の音量調整をする場合は 0..1f までの値を入れる（BGMうるさい場合など）
 */
class AudioMixingProcessor(
    private val audioFileList: List<File>,
    private val resultFile: File,
    private val tempWorkFolder: File,
    private val audioCodec: String? = null,
    private val audioDurationMs: Int? = null,
    private val bitRate: Int? = null,
    private val samplingRate: Int? = null,
    private val mixingVolume: Float = 0.25f
) {

    /** 処理を始める、終わるまで一時停止します */
    suspend fun start() = withContext(Dispatchers.Default) {
        // 特に時間が指定されていない場合は最初のファイルの時間を取り出す
        // 単位はマイクロ秒
        val audioDurationUs = (audioDurationMs?.times(1000L)) ?: MediaExtractorTool.extractMedia(audioFileList.first().path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO)?.let { (mediaExtractor, _, format) ->
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            mediaExtractor.release()
            return@let durationUs
        } ?: return@withContext

        // それぞれのファイルをデコードして PCM にする
        // TODO MediaCodec の起動上限に引っかかりそう
        Log.d(TAG, "PCMへデコード開始")
        val pcmDecodedFileList = audioFileList.mapIndexed { fileIndex, audioFile ->
            async {
                // 情報を読み出す
                val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(audioFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO)!!
                mediaExtractor.selectTrack(index)
                // 仮の保存先を作成
                val rawTempFile = createTempFile("$TEMP_FILE_NAME_PREFIX$fileIndex")
                // デコーダー起動
                val audioDecoder = AudioDecoder()
                audioDecoder.prepareDecoder(format)
                audioDecoder.startAudioDecode(
                    readSampleData = { byteBuffer ->
                        val size = mediaExtractor.readSampleData(byteBuffer, 0)
                        mediaExtractor.advance()
                        return@startAudioDecode if (mediaExtractor.sampleTime < audioDurationUs) {
                            // 動画時間の方がまだ長い場合は継続。動画のほうが短くても終わる
                            size to mediaExtractor.sampleTime
                        } else {
                            // 動画より音声のほうが長くなる場合は終わらせる
                            0 to 0
                        }
                    },
                    onOutputBufferAvailable = { bytes -> rawTempFile.appendBytes(bytes) }
                )
                audioDecoder.release()
                return@async rawTempFile
            }
        }.map { it.await() }

        // 音声を合成する
        // 以下のように、同じ位置のバイトを各ファイル取得して、全部足していく
        // File-1 0x00 0x00 0x00 ...
        // File-2 0x01 0x01 0x01 ...
        // File-3 0x02 0x02 0x02 ...
        // -------------------------
        // Result 0x03 0x03 0x03 ...
        Log.d(TAG, "ミキシング開始")
        val mixingRawFile = createTempFile("${TEMP_FILE_NAME_PREFIX}mixed_raw_file")
        val inputStreamList = pcmDecodedFileList.map { it.inputStream() }
        while (isActive) {
            val fileToByteArray = withContext(Dispatchers.IO) {
                inputStreamList.map { ByteArray(8192).also { bytes -> it.read(bytes) } }
            }
            // 音を重ねていく
            val mixingRawAudio = (0 until 8192)
                .map { index ->
                    fileToByteArray
                        .mapIndexed { fileIndex, byteArray ->
                            if (fileIndex == 0) {
                                byteArray[index]
                            } else {
                                // 動画本編以外の音量を調整する
                                (byteArray[index] * mixingVolume).toInt().toByte()
                            }
                        }
                        .sum()
                        .toByte()
                }
                .toByteArray()
            mixingRawFile.appendBytes(mixingRawAudio)
            // ない場合は終了
            if (inputStreamList.any { it.available() == 0 }) {
                break
            }
        }
        withContext(Dispatchers.IO) {
            inputStreamList.map { it.close() }
        }

        // 生ファイルをエンコードする
        Log.d(TAG, "AACにエンコード開始")
        val mixedRawAudioInputStream = mixingRawFile.inputStream()
        // コンテナフォーマット
        val isOpus = audioCodec == MediaFormat.MIMETYPE_AUDIO_OPUS
        val containerFormat = if (isOpus) MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM else MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)
        var trackIndex = -1
        // エンコーダー起動
        val audioEncoder = AudioEncoder()
        val samplingRate = samplingRate ?: if (isOpus) 48_000 else 44_100
        audioEncoder.prepareEncoder(
            sampleRate = samplingRate,
            channelCount = 2,
            bitRate = bitRate ?: 192_000,
            isOpus = isOpus
        )
        audioEncoder.startAudioEncode(
            onRecordInput = { byteArray -> mixedRawAudioInputStream.read(byteArray) },
            onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                if (trackIndex != -1) {
                    mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo)
                }
            },
            onOutputFormatAvailable = {
                trackIndex = mediaMuxer.addTrack(it)
                mediaMuxer.start()
            },
        )
        mediaMuxer.release()
        audioEncoder.release()
        withContext(Dispatchers.IO) {
            mixedRawAudioInputStream.close()
        }
    }

    /**
     * 一時ファイルを作成する
     *
     * @param fileName ファイル名
     */
    private suspend fun createTempFile(fileName: String) = withContext(Dispatchers.IO) {
        return@withContext File(tempWorkFolder, fileName).apply {
            createNewFile()
        }
    }

    companion object {
        private val TAG = AudioMixingProcessor::class.java.simpleName

        /** 仮のファイルの名前のプレフィックス */
        private const val TEMP_FILE_NAME_PREFIX = "raw_audio_file_"
    }
}