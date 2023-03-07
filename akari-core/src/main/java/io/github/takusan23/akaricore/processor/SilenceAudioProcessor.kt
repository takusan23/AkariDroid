package io.github.takusan23.akaricore.processor

import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.akaricore.common.AudioEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 無音の音声ファイルを作成する */
object SilenceAudioProcessor {

    /** トラック番号が空の場合 */
    private const val UNDEFINED_TRACK_INDEX = -1

    /**
     * 音声を生成する
     *
     * @param resultFile 出力ファイル
     * @param durationMs 音声の時間（ミリ秒）
     * @param audioCodec 音声コーデック
     * @param containerFormat コンテナフォーマット
     * @param bitRate ビットレート
     * @param samplingRate サンプリングレート
     */
    suspend fun start(
        resultFile: File,
        durationMs: Long,
        audioCodec: String = MediaFormat.MIMETYPE_AUDIO_AAC,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        bitRate: Int = 128_000,
        samplingRate: Int = 44_100,
    ) = withContext(Dispatchers.Default) {
        // コンテナフォーマット
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)
        var trackIndex = UNDEFINED_TRACK_INDEX

        // 空の音声を用意する
        // PCMは 2 バイト利用する
        // ステレオなので 2 チャンネル
        // それをサンプリングレートでかけると、1秒間に必要なバイトサイズが出る
        // そして最後に秒をかけて、無音のPCMデータを作成する
        // TODO メモリにPCMデータを載せておくのはもったいないことしてるかもしれない
        val second = (durationMs / 1_000f).toInt()
        val pcmByteSize = 2 * 2 * samplingRate
        val audioInputStream = ByteArray(pcmByteSize * second).inputStream()

        // エンコーダー
        val audioEncoder = AudioEncoder()
        audioEncoder.prepareEncoder(
            codec = audioCodec,
            sampleRate = samplingRate,
            channelCount = 2,
            bitRate = bitRate,
        )
        audioEncoder.startAudioEncode(
            onRecordInput = { byteArray -> audioInputStream.read(byteArray) },
            onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                if (trackIndex != UNDEFINED_TRACK_INDEX) {
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
    }

}