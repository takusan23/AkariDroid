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
import java.io.InputStream

/**
 * 音声をエンコードする。
 * 複数ファイルを指定すると音声を重ねてエンコードする。
 *
 * 音の合成ですが、難しいことはなく、波の音を重ねることで音がミックスされます。
 * 実装的には、AACをPCMにして（AudacityのRawデータのように）、バイト配列から同じ位置のByteを足すことでできます。
 */
object AudioMixingProcessor {

    private val TAG = AudioMixingProcessor::class.java.simpleName

    /** ByteArray のサイズ */
    private const val READ_BYTE_SIZE = 8192

    /** 仮のファイルの名前のプレフィックス */
    private const val TEMP_FILE_NAME_PREFIX = "raw_audio_file_"

    /** トラック番号が空の場合 */
    private const val UNDEFINED_TRACK_INDEX = -1

    /**
     * 処理を始める、終わるまで一時停止します
     *
     * @param audioFileList 重ねる音声ファイル。ファイルと音声再生位置
     * @param tempFolder 一時ファイル置き場
     * @param audioCodec 音声コーデック
     * @param containerFormat コンテナフォーマット
     * @param audioDurationMs 音声ファイルの時間
     * @param bitRate ビットレート
     * @param samplingRate サンプリングレート
     * @param mixingVolume ミックスする素材の音量。二番目以降のファイルに適用される
     */
    suspend fun start(
        audioFileList: List<MixingFileData>,
        resultFile: File,
        tempFolder: File,
        audioDurationMs: Long,
        audioCodec: String = MediaFormat.MIMETYPE_AUDIO_AAC,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        bitRate: Int = 128_000,
        samplingRate: Int = 44_100
    ) = withContext(Dispatchers.Default) {
        // それぞれのファイルをデコードして PCM にする
        // TODO MediaCodec の起動上限に引っかかりそう
        Log.d(TAG, "PCMへデコード開始")
        val pcmDecodedFileList = audioFileList.mapIndexed { fileIndex, mixData ->
            async {
                // 情報を読み出す
                val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(mixData.audioFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO)!!
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
                        return@startAudioDecode size to mediaExtractor.sampleTime
                    },
                    onOutputBufferAvailable = { bytes -> rawTempFile.appendBytes(bytes) }
                )
                audioDecoder.release()
                // コピーしてファイルパスを デコードしたPCM にする
                return@async mixData.copy(audioFile = rawTempFile)
            }
        }.map { it.await() }.toMutableList()

        // 音声の時間だけ無音のPCM音声ファイルを作る
        // この何もない音声ファイルに対して音声を足していく
        val silenceAudioFile = tempFolder.resolve("${TEMP_FILE_NAME_PREFIX}_silence_audio_file")
        val second = (audioDurationMs / 1_000f).toInt()
        val pcmByteSize = 2 * 2 * samplingRate
        ByteArray(pcmByteSize * second).inputStream().use { inputStream ->
            silenceAudioFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        pcmDecodedFileList.add(MixingFileData(silenceAudioFile, 0..audioDurationMs, 1f))

        // PCM の 1秒間に必要なサイズ
        // PCM は 1 チャンネルで 2 バイト利用する。
        // ステレオなので 2 チャンネルにする。
        // あとはサンプリングレートをかけて完成。
        val pcmSecondByteSize = 2 * 2 * samplingRate

        // 音声を合成する
        // 以下のように、同じ位置のバイトを各ファイル取得して、全部足していく
        // File-1 0x00 0x00 0x00 ...
        // File-2 0x01 0x01 0x01 ...
        // File-3 0x02 0x02 0x02 ...
        // -------------------------
        // Result 0x03 0x03 0x03 ...
        Log.d(TAG, "ミキシング開始")
        val mixingRawFile = tempFolder.resolve("${TEMP_FILE_NAME_PREFIX}mixed_raw_file")
        val mixingRawDataList = pcmDecodedFileList.map { mixData ->
            // timeRange はミリ秒だったので秒にする
            val durationSec = (mixData.timeRange.last - mixData.timeRange.first) / 1_000f
            val startSec = mixData.timeRange.first / 1_000f
            // 開始時間を先頭から何バイト分あるかに変換する
            val skipByteSize = (pcmSecondByteSize * startSec).toLong()
            // 同様に時間も何バイト分かどうか
            val mixingByteSize = (pcmSecondByteSize * durationSec).toLong()

            MixingRawData(mixData.audioFile.inputStream(), skipByteSize, mixingByteSize, mixData.volume)
        }

        var prevReadByteSize = 0L
        while (isActive) {
            // 重ねるファイルの個数だけ PCM を ByteArray で取得し配列に入れる
            val mixingRawByteArrayList = mixingRawDataList.filter {
                // 書き込み済みバイトサイズをもとにミックス対象のファイルを見つける
                prevReadByteSize in it.skipByteSize..(it.skipByteSize + it.mixingByteSize)
            }.map { rawData ->
                ByteArray(READ_BYTE_SIZE)
                    .also { bytes -> rawData.inputStream.read(bytes) }
                    // 音量調整をする
                    .map { (it * rawData.volume).toInt().toByte() }
            }
            // もうミックス対象がない場合は終了にする
            if (mixingRawByteArrayList.isEmpty()) {
                break
            }

            // 音を重ねていく
            val mixingRawAudio = (0 until READ_BYTE_SIZE)
                .map { index ->
                    // ここで複数の PCM 音声 を重ねている
                    // 音声は波で、波の合成は単純な足し算らしい（物理分からん）
                    mixingRawByteArrayList
                        // 範囲外になるタイミングがあるので
                        .map { byteArray -> byteArray.getOrNull(index) ?: 0x00 }
                        .sum()
                        .toByte()
                }
                .toByteArray()
            mixingRawFile.appendBytes(mixingRawAudio)
            prevReadByteSize += READ_BYTE_SIZE
        }
        withContext(Dispatchers.IO) {
            mixingRawDataList.forEach { it.inputStream.close() }
        }

        // 生ファイルをエンコードする
        Log.d(TAG, "AACにエンコード開始")
        val mixedRawAudioInputStream = mixingRawFile.inputStream()
        // コンテナフォーマット
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)
        var trackIndex = UNDEFINED_TRACK_INDEX
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

    /**
     * ミックスするファイルの情報
     *
     * @param audioFile ファイル
     * @param timeRange 再生開始位置から終了位置
     * @param volume ボリューム 0..1 まで
     */
    data class MixingFileData(
        val audioFile: File,
        val timeRange: LongRange,
        val volume: Float = 1f
    )

    /**
     * ミックス処理の際に内部で利用するデータクラス
     */
    private data class MixingRawData(
        val inputStream: InputStream,
        val skipByteSize: Long,
        val mixingByteSize: Long,
        val volume: Float
    )

}