package io.github.takusan23.akaricore.v1.tool

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * 音声と映像をコンテナフォーマットへしまって一つの動画にする関数がある
 * */
object MediaMuxerTool {

    /**
     * コンテナフォーマットへ格納する
     *
     * @param resultFile 最終的なファイル
     * @param containerFormat [MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4] など
     * @param mergeFileList コンテナフォーマットへ入れる音声、映像データの[File]
     * */
    @SuppressLint("WrongConstant")
    suspend fun mixed(
        resultFile: File,
        containerFormat: Int,
        mergeFileList: List<File>,
    ) = withContext(Dispatchers.Default) {
        // 映像と音声を追加して一つの動画にする
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)

        // 音声、映像ファイルの トラック番号 と [MediaExtractor] の Pair
        val trackIndexToExtractorPairList = mergeFileList.map {
            // MediaExtractorとフォーマット取得
            val mediaExtractor = MediaExtractor().apply { setDataSource(it.path) }
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
            val byteBuffer = ByteBuffer.allocate(1024 * 4096)
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