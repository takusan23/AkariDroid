package io.github.takusan23.akaricore.processor

import android.media.*
import io.github.takusan23.akaricore.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 音声をエンコードする
 *
 * @param videoFile フィルターをかけたい動画ファイル
 * @param resultFile エンコードしたファイルの保存先
 * @param audioCodec エンコード後の 音声コーデック [MediaFormat.MIMETYPE_AUDIO_OPUS] など
 * @param containerFormat コンテナフォーマット [MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4] など
 * @param bitRate ビットレート
 * */
class AudioProcessor(
    private val videoFile: File,
    private val resultFile: File,
    private val audioCodec: String? = null,
    private val containerFormat: Int? = null,
    private val bitRate: Int? = null,
) {
    /** コンテナフォーマットへ格納するやつ */
    private val mediaMuxer by lazy { MediaMuxer(resultFile.path, containerFormat ?: MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4) }

    /** データを取り出すやつ */
    private var currentMediaExtractor: MediaExtractor? = null

    /** エンコード用 [MediaCodec] */
    private var encodeMediaCodec: MediaCodec? = null

    /** デコード用 [MediaCodec] */
    private var decodeMediaCodec: MediaCodec? = null

    /** 処理を始める、終わるまで一時停止します */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun start() = withContext(Dispatchers.Default) {

        // 動画の情報を読み出す
        val (mediaExtractor, index, format) = MediaExtractorTool.extractMedia(videoFile.path, MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO) ?: return@withContext
        currentMediaExtractor = mediaExtractor
        // 音声のトラックを選択
        mediaExtractor.selectTrack(index)
        mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        // 解析結果から各パラメータを取り出す
        val decodeMimeType = format.getString(MediaFormat.KEY_MIME)!!
        val encodeMimeType = audioCodec ?: decodeMimeType
        val samplingRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        // デコード用（aac -> 生データ）MediaCodec
        decodeMediaCodec = MediaCodec.createDecoderByType(decodeMimeType).apply {
            // デコード時は MediaExtractor の MediaFormat で良さそう
            configure(format, null, null, 0)
        }

        var audioTrackIndex = UNDEFINED_TRACK_INDEX

        // エンコード用（生データ -> aac）MediaCodec
        encodeMediaCodec = MediaCodec.createEncoderByType(encodeMimeType).apply {
            // エンコーダーにセットするMediaFormat
            val audioMediaFormat = MediaFormat.createAudioFormat(encodeMimeType, samplingRate, channelCount).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate ?: 192_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, INPUT_BUFFER_SIZE)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }
            configure(audioMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }

        // nonNull
        val decodeMediaCodec = decodeMediaCodec!!
        val encodeMediaCodec = encodeMediaCodec!!
        // スタート
        decodeMediaCodec.start()
        encodeMediaCodec.start()

        // 再生位置など
        val bufferInfo = MediaCodec.BufferInfo()

        // 読み出し済みの位置と時間
        var totalBytesRead = 0
        var presentationTime = 0L

        while (isActive) {
            // もし -1 が返ってくれば configure() が間違ってる
            val inputBufferId = decodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferId >= 0) {
                // Extractorからデータを読みだす
                val inputBuffer = decodeMediaCodec.getInputBuffer(inputBufferId)!!
                val size = mediaExtractor.readSampleData(inputBuffer, 0)
                if (size > 0) {
                    // デコーダーへ流す
                    decodeMediaCodec.queueInputBuffer(inputBufferId, 0, size, mediaExtractor.sampleTime, 0)
                    mediaExtractor.advance()
                } else {
                    // データなくなった場合は終了フラグを立てる
                    decodeMediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    // 開放
                    mediaExtractor.release()
                    // 終了
                    break
                }
            }

            // デコード結果を受け取って、一時的に保存する
            val outputBufferId = decodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (outputBufferId >= 0) {
                // デコード結果をもらう
                val outputBuffer = decodeMediaCodec.getOutputBuffer(outputBufferId)!!
                val outputSize = bufferInfo.size
                val buffer = ByteArray(outputSize)
                outputBuffer[buffer]

                // 生データをエンコードする
                var prevReadOffset = 0
                while (isActive) {
                    val inputBufferId = encodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferId >= 0) {
                        val inputBuffer = encodeMediaCodec.getInputBuffer(inputBufferId)!!
                        // 書き込む
                        inputBuffer.put(buffer, 0, outputSize)
                        encodeMediaCodec.queueInputBuffer(inputBufferId, 0, outputSize, presentationTime, 0)
                        prevReadOffset += outputSize
                        totalBytesRead += outputSize
                        // あんまり分からん
                        presentationTime = 1000000L * (totalBytesRead / 2) / (samplingRate * channelCount)
                        // もうない場合
                        if (prevReadOffset >= outputSize) {
                            break
                        }
                    }
                    // 出力
                    val outputBufferId = encodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    if (outputBufferId >= 0) {
                        val outputBuffer = encodeMediaCodec.getOutputBuffer(outputBufferId)!!
                        if (bufferInfo.size > 1) {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && audioTrackIndex != UNDEFINED_TRACK_INDEX) {
                                // ファイルに書き込む...
                                // MediaMuxer へ addTrack した後
                                mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                            }
                        } else {
                            // もう無い！
                            break
                        }
                        // 返却
                        encodeMediaCodec.releaseOutputBuffer(outputBufferId, false)
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && audioTrackIndex == UNDEFINED_TRACK_INDEX) {
                        // MediaMuxerへ音声トラックを追加するのはこのタイミングで行う
                        // 最初だけ
                        // 音声の場合は多分 MediaExtractor をそのまま入れてもいいはずですが
                        // 音声コーデックを変更できるようにしたため
                        val newFormat = encodeMediaCodec.outputFormat
                        audioTrackIndex = mediaMuxer.addTrack(newFormat)
                        mediaMuxer.start()
                    }
                }

                // 消したほうがいいらしい
                outputBuffer.clear()
                // 返却
                decodeMediaCodec.releaseOutputBuffer(outputBufferId, false)
            }
        }

        // デコーダー終了
        decodeMediaCodec.stop()
        decodeMediaCodec.release()
        // エンコーダー終了
        encodeMediaCodec.stop()
        encodeMediaCodec.release()

        // MediaMuxerも終了
        // MediaMuxer#stopでコケる場合、大体MediaFormatのパラメータ不足です。
        // MediaExtractorで出てきたFormatを入れると直ると思います。
        mediaMuxer.stop()
        mediaMuxer.release()
    }

    /** 強制終了時に呼ぶ */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun stop() {
        // すでにstopしてると例外を投げるので
        try {
            decodeMediaCodec?.stop()
            decodeMediaCodec?.release()
            encodeMediaCodec?.stop()
            encodeMediaCodec?.release()
            currentMediaExtractor?.release()
            mediaMuxer.stop()
            mediaMuxer.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        /** タイムアウト */
        private const val TIMEOUT_US = 10000L

        /** MediaCodecでもらえるInputBufferのサイズ */
        private const val INPUT_BUFFER_SIZE = 655360

        /** トラック番号が空の場合 */
        private const val UNDEFINED_TRACK_INDEX = -1
    }
}