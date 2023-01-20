package io.github.takusan23.akaridroid.data

import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.serialization.Serializable

/**
 * エンコーダーに渡すそれぞれの値
 *
 * @param videoCodec 動画コーデック。H.264とかVP9とか
 * @param videoWidth 動画の幅
 * @param videoHeight 動画の高さ
 * @param frameRate フレームレート。fps
 * @param bitRate ビットレート
 */
@Serializable
data class VideoOutputFormat(
    val videoCodec: VideoCodec = VideoCodec.AVC,
    val videoWidth: Int = 1280,
    val videoHeight: Int = 720,
    val frameRate: Int = 30,
    val bitRate: Int = 5_000_000, // 5Mbps
)

/**
 * 動画コーデックとコンテナフォーマット
 *
 * @param videoMediaCodecMimeType 映像をエンコードする MediaCodec の MIME-TYPE
 * @param audioMediaCodecMimeType 音声をエンコードする MediaCodec の MIME-TYPE
 * @param containerFormat コンテナフォーマット。VP9はwebm、avcはmp4なので
 */
enum class VideoCodec(val videoMediaCodecMimeType: String, val audioMediaCodecMimeType: String, val containerFormat: ContainerFormat) {
    /** avc 別名 H.264 */
    AVC(MediaFormat.MIMETYPE_VIDEO_AVC, MediaFormat.MIMETYPE_AUDIO_AAC, ContainerFormat.MP4),

    /** hevc 別名 H.265 */
    HEVC(MediaFormat.MIMETYPE_VIDEO_HEVC, MediaFormat.MIMETYPE_AUDIO_AAC, ContainerFormat.MP4),

    /** VP9。OPUSはサンプリングレートがAACと違うので core 直す必要あり */
    VP9(MediaFormat.MIMETYPE_VIDEO_VP9, MediaFormat.MIMETYPE_AUDIO_OPUS, ContainerFormat.WEBM)
}

/**
 * コンテナフォーマット
 * 圧縮した映像と音声を一つに保存するためのもの
 *
 * @param mediaMuxerVal MediaMuxer での値
 */
enum class ContainerFormat(val mediaMuxerVal: Int) {
    /** mp4 */
    MP4(MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4),

    /** webm */
    WEBM(MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM)
}