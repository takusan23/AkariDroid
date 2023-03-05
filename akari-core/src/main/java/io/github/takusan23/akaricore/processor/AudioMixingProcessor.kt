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
 */
object AudioMixingProcessor {

    private val TAG = AudioMixingProcessor::class.java.simpleName

    /** 仮のファイルの名前のプレフィックス */
    private const val TEMP_FILE_NAME_PREFIX = "raw_audio_file_"

    /**
     * 処理を始める、終わるまで一時停止します
     *
     * @param audioFileList 重ねる素材ファイル
     * @param resultFile 出力ファイル
     * @param tempFolder 一時ファイル置き場
     * @param audioCodec 音声コーデック
     * @param containerFormat コンテナフォーマット
     * @param audioDurationMs 時間。していなければ最初のファイルの時間
     * @param bitRate ビットレート
     * @param samplingRate サンプリングレート
     * @param mixingVolume ミックスする素材の音量。二番目以降のファイルに適用される
     */
    suspend fun start(
        audioFileList: List<File>,
        resultFile: File,
        tempFolder: File,
        audioCodec: String = MediaFormat.MIMETYPE_AUDIO_AAC,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        audioDurationMs: Int? = null,
        bitRate: Int = 128_000,
        samplingRate: Int = 44_100,
        mixingVolume: Float = 0.25f
    ) = withContext(Dispatchers.Default) {
        // 特に時間が指定されていない場合は最初のファイルの時間を取り出す
        // 単位はマイクロ秒
        val audioDurationUs = audioDurationMs?.let { it * 1000L } ?: MediaExtractorTool.extractMedia(audioFileList.first().path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO)!!.let { (mediaExtractor, _, format) ->
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            mediaExtractor.release()
            return@let durationUs
        }
        // それぞれのファイルをデコードして PCM にする
        // TODO MediaCodec の起動上限に引っかかりそう
        Log.d(TAG, "PCMへデコード開始")
        val pcmDecodedFileList = audioFileList.mapIndexed { fileIndex, audioFile ->
            async {
                // 情報を読み出す
                val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(audioFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO)!!
                mediaExtractor.selectTrack(index)
                // 仮の保存先を作成
                val rawTempFile = tempFolder.resolve("$TEMP_FILE_NAME_PREFIX$fileIndex")
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
        val mixingRawFile = tempFolder.resolve("${TEMP_FILE_NAME_PREFIX}mixed_raw_file")
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
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)
        var trackIndex = -1
        // エンコーダー起動
        val audioEncoder = AudioEncoder()
        audioEncoder.prepareEncoder(
            codec = audioCodec,
            sampleRate = samplingRate,
            channelCount = 2,
            bitRate = bitRate,
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
}