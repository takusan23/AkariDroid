package io.github.takusan23.akaridroid.encoder

import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import androidx.annotation.RequiresApi

/** エンコーダーの設定をまとめたクラス */
sealed interface EncoderParameters {

    /** コンテナフォーマット */
    val containerFormat: ContainerFormat

    /** 音声のみ（音声のみとか使うの？） */
    data class AudioOnly(
        override val containerFormat: ContainerFormat,
        val audioEncoderParameters: AudioEncoderParameters
    ) : EncoderParameters

    /** 音声と映像 */
    data class AudioVideo(
        override val containerFormat: ContainerFormat,
        val videoEncoderParameters: VideoEncoderParameters,
        val audioEncoderParameters: AudioEncoderParameters
    ) : EncoderParameters

    /**
     * 動画エンコーダー設定
     *
     * @param codec 動画コーデック
     * @param bitrate ビットレート
     * @param frameRate フレームレート
     * @param keyframeInterval キーフレーム間隔
     */
    data class VideoEncoderParameters(
        val codec: VideoCodec,
        val bitrate: Int,
        val frameRate: Int,
        val keyframeInterval: Int
    )

    /**
     * 音声エンコーダー設定
     *
     * @param codec 音声コーデック
     * @param bitrate ビットレート
     */
    data class AudioEncoderParameters(
        val codec: AudioCodec,
        val bitrate: Int
    )

    /**
     * コンテナフォーマット
     *
     * @param extension 拡張子
     * @param androidMediaMuxerFormat [MediaMuxer]に渡す際の値
     */
    enum class ContainerFormat(val extension: String, val androidMediaMuxerFormat: Int) {

        /** mp4 コンテナ */
        MP4("mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4),

        /** webm コンテナ */
        WEBM("webm", MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM)
    }

    /**
     * 動画コーデック
     *
     * @param androidMediaCodecName [android.media.MediaCodec.createEncoderByType]に渡す際の値
     */
    enum class VideoCodec(val androidMediaCodecName: String) {

        /** AVC（H.264）。互換性ではぴかいち。ビットレートを高くしないと行けないので、ファイルサイズが大きくなりがち。 */
        AVC(MediaFormat.MIMETYPE_VIDEO_AVC),

        /** HEVC（H.265）。特許が意味不明で、誰にお金を払えば良いのか、そもそも払わないといけないのか分からない。法律に詳しくない場合は使わないほうがいい。 */
        HEVC(MediaFormat.MIMETYPE_VIDEO_HEVC),

        /** WEBM コンテナに入れたければ。 */
        VP9(MediaFormat.MIMETYPE_VIDEO_VP9),

        /** 期待の新星 AV1。Pixel 8 以降はハードウェアエンコードが利用できます。Android 14 からソフトウェアエンコードが利用できます。 */
        @RequiresApi(Build.VERSION_CODES.Q)
        AV1(MediaFormat.MIMETYPE_VIDEO_AV1)
    }

    /**
     * 音声コーデック
     *
     * @param androidMediaCodecName [android.media.MediaCodec.createEncoderByType]に渡す際の値
     */
    enum class AudioCodec(val androidMediaCodecName: String) {
        /** AAC */
        AAC(MediaFormat.MIMETYPE_AUDIO_AAC),

        /** OPUS（WebM 用） */
        OPUS(MediaFormat.MIMETYPE_AUDIO_OPUS)
    }

    /** 画質のおまかせ設定を用意する。松竹梅。macbook のマシンスペックを松竹梅で言うのすきだよ */
    companion object {

        /** 音声のみ */
        val AUDIO_ONLY_PRESET = AudioOnly(
            containerFormat = ContainerFormat.MP4,
            audioEncoderParameters = AudioEncoderParameters(codec = AudioCodec.AAC, bitrate = 128_000)
        )

        /** 低画質 */
        val LOW_QUALITY = AudioVideo(
            containerFormat = ContainerFormat.MP4,
            videoEncoderParameters = VideoEncoderParameters(codec = VideoCodec.AVC, bitrate = 2_000_000, frameRate = 30, keyframeInterval = 1),
            audioEncoderParameters = AudioEncoderParameters(codec = AudioCodec.AAC, bitrate = 128_000)
        )

        /** 中画質 */
        val MEDIUM_QUALITY = AudioVideo(
            containerFormat = ContainerFormat.MP4,
            videoEncoderParameters = VideoEncoderParameters(codec = VideoCodec.AVC, bitrate = 6_000_000, frameRate = 30, keyframeInterval = 1),
            audioEncoderParameters = AudioEncoderParameters(codec = AudioCodec.AAC, bitrate = 128_000)
        )

        /** 高画質。解像度を見ていないので、これを選んで微調整すればいいと思う。 */
        val HIGH_QUALITY = AudioVideo(
            containerFormat = ContainerFormat.MP4,
            videoEncoderParameters = VideoEncoderParameters(codec = VideoCodec.AVC, bitrate = 12_000_000, frameRate = 30, keyframeInterval = 1),
            audioEncoderParameters = AudioEncoderParameters(codec = AudioCodec.AAC, bitrate = 128_000)
        )

    }
}