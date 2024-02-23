package io.github.takusan23.akaridroid.preview

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build


/**
 * PCM のバイト配列を再生する。AudioTrack のラッパー
 * MediaPlayer の PCM 音声データ再生版。
 *
 * @param samplingRate PCM 音声データのサンプリングレート
 * @param channelCount PCM 音声データのチャンネル数
 * @param bitDepth PCM 音声データの量子化ビット数
 */
class PcmPlayer(
    samplingRate: Int,
    channelCount: Int,
    bitDepth: Int
) {

    private val minBufferSize = AudioTrack.getMinBufferSize(
        samplingRate,
        if (channelCount == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO,
        if (bitDepth == 2) AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT
    )

    private val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AudioTrack.Builder().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            setBufferSizeInBytes(
                AudioTrack.getMinBufferSize(
                    samplingRate,
                    if (channelCount == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO,
                    if (bitDepth == 2) AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT
                )
            )
        }.build()
    } else {
        AudioTrack(
            AudioManager.STREAM_MUSIC,
            samplingRate,
            if (channelCount == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO,
            if (bitDepth == 2) AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_8BIT,
            minBufferSize,
            AudioTrack.MODE_STREAM
        )
    }

    /** PCM 音声データのバイト配列を流す */
    fun writePcmData(byteArray: ByteArray) {
        audioTrack.write(byteArray, 0, byteArray.size)
    }

    /** 再生する */
    fun play() {
        audioTrack.play()
    }

    /** 一時停止する */
    fun pause() {
        audioTrack.pause()
        // flush して再生しきれなかったデータを捨てる
        audioTrack.flush()
    }

    /** 破棄する */
    fun destroy() {
        audioTrack.stop()
        audioTrack.release()
    }

}