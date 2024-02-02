package io.github.takusan23.akaricore.v1.processor

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import io.github.takusan23.akaricore.v1.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/** 動画、音声を指定時間でカットする処理 */
object CutProcessor {

    /**
     * 動画、音声を指定時間で切り抜いて返す
     *
     * @param targetVideoFile 対象のファイル
     * @param resultFile 出力ファイル
     * @param timeRangeMs
     */
    @SuppressLint("WrongConstant")
    suspend fun cut(
        targetVideoFile: File,
        resultFile: File,
        timeRangeMs: LongRange,
        extractMimeType: MediaExtractorTool.ExtractMimeType,
        containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
    ) = withContext(Dispatchers.Default) {
        val (mediaExtractor, extractorIndex, format) = MediaExtractorTool.extractMedia(targetVideoFile.path, extractMimeType)!!
        mediaExtractor.selectTrack(extractorIndex)
        // 保存先
        val mediaMuxer = MediaMuxer(resultFile.path, containerFormat)
        val mixerIndex = mediaMuxer.addTrack(format)
        mediaMuxer.start()
        // シークする
        mediaExtractor.seekTo(timeRangeMs.first * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val byteBuffer = ByteBuffer.allocate(1024 * 4096)
        val bufferInfo = MediaCodec.BufferInfo()
        while (isActive) {
            // データを読み出す
            val offset = byteBuffer.arrayOffset()
            bufferInfo.size = mediaExtractor.readSampleData(byteBuffer, offset)
            // もう無い場合
            if (bufferInfo.size < 0) break
            bufferInfo.presentationTimeUs = mediaExtractor.sampleTime
            bufferInfo.flags = mediaExtractor.sampleFlags // Lintがキレるけど黙らせる
            // 時間超えた場合は終了
            if (bufferInfo.presentationTimeUs > timeRangeMs.last * 1000) break
            // 書き込む
            mediaMuxer.writeSampleData(mixerIndex, byteBuffer, bufferInfo)
            // 次のデータに進める
            mediaExtractor.advance()
        }

        // あとしまつ
        mediaExtractor.release()
        mediaMuxer.stop()
        mediaMuxer.release()
    }

}