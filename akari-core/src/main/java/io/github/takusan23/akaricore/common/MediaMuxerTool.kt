package io.github.takusan23.akaricore.common

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaMuxer
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * 音声と映像をコンテナフォーマットへしまって一つの動画にする関数がある
 * */
object MediaMuxerTool {

    private const val BUFFER_SIZE = 1024 * 4096

    /**
     * [AkariCoreInputOutput.Output]に対応した[MediaMuxer]
     *
     * - 注意点です
     * - [AkariCoreInputOutput.JavaFile]
     *  - ぶっちゃけこれしか対応してません
     * - [AkariCoreInputOutput.AndroidUri]
     *  - Android 8 以降のみです
     * - それ以外
     *  - 対応してません；；
     *
     * @param output 保存先
     * @param format コンテナフォーマット
     */
    fun createMediaMuxer(
        output: AkariCoreInputOutput.Output,
        format: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
    ): MediaMuxer {
        return when (output) {
            is AkariCoreInputOutput.JavaFile -> MediaMuxer(output.filePath, format)

            is AkariCoreInputOutput.AndroidUri -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                output.getWritableFileDescriptor().use {
                    MediaMuxer(it.fileDescriptor, format)
                }
            } else {
                throw UnsupportedOperationException("未対応")
            }

            is AkariCoreInputOutput.OutputJavaByteArray -> throw UnsupportedOperationException("未対応")
        }
    }

    /**
     * コンテナフォーマットへ格納する
     *
     * @param output 最終的なファイル
     * @param containerFormat [MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4] など
     * @param containerFormatTrackInputList コンテナフォーマットへ入れる音声、映像データの[File]
     * */
    @SuppressLint("WrongConstant")
    suspend fun mixed(
        output: AkariCoreInputOutput.Output,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
        containerFormatTrackInputList: List<AkariCoreInputOutput.Input>,
    ) = withContext(Dispatchers.Default) {
        // 映像と音声を追加して一つの動画にする
        val mediaMuxer = createMediaMuxer(output, containerFormat)

        // 音声、映像ファイルの トラック番号 と [MediaExtractor] の Pair
        val trackIndexToExtractorPairList = containerFormatTrackInputList.map {
            // MediaExtractorとフォーマット取得
            val mediaExtractor = MediaExtractorTool.createMediaExtractor(it)
            val mediaFormat = mediaExtractor.getTrackFormat(0) // 音声には音声、映像には映像しか無いので 0
            mediaExtractor.selectTrack(0)
            mediaFormat to mediaExtractor
        }.map { (format, extractor) ->
            // フォーマットをMediaMuxerに渡して、トラックを追加してもらう
            val videoTrackIndex = mediaMuxer.addTrack(format)
            videoTrackIndex to extractor
        }
        // MediaMuxerスタート
        mediaMuxer.start()
        // 映像と音声を一つの動画ファイルに書き込んでいく
        trackIndexToExtractorPairList.forEach { (index, extractor) ->
            val byteBuffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()
            // データが無くなるまで回す
            while (isActive) {
                // データを読み出す
                val offset = byteBuffer.arrayOffset()
                bufferInfo.size = extractor.readSampleData(byteBuffer, offset)
                // もう無い場合
                if (bufferInfo.size < 0) break
                // 書き込む
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags // Lintがキレるけど黙らせる
                mediaMuxer.writeSampleData(index, byteBuffer, bufferInfo)
                // 次のデータに進める
                extractor.advance()
            }
            // あとしまつ
            extractor.release()
        }
        // あとしまつ
        mediaMuxer.stop()
        mediaMuxer.release()
    }

}