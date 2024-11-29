package io.github.takusan23.akaricore.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.common.MediaMuxerTool
import java.nio.ByteBuffer

/**
 * [AkariEncodeMuxerInterface]を Android にある[MediaMuxer]で実装したもの
 *
 * @param output ファイル保存先
 * @param containerFormat コンテナフォーマット
 */
class AkariAndroidMuxer(
    output: AkariCoreInputOutput.Output,
    containerFormat: Int = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
) : AkariEncodeMuxerInterface {

    /** コンテナフォーマットのマルチプレクサ */
    private val mediaMuxer = MediaMuxerTool.createMediaMuxer(output, containerFormat)

    /** トラックのインデックス */
    private var videoTrackIndex = -1

    override suspend fun onOutputFormat(mediaFormat: MediaFormat) {
        videoTrackIndex = mediaMuxer.addTrack(mediaFormat)
        mediaMuxer.start()
    }

    override suspend fun onOutputData(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
    }

    override suspend fun stop() {
        mediaMuxer.stop()
        mediaMuxer.release()
    }
}