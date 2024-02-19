package io.github.takusan23.akaricore.v2.audio

/** この アプリ / ライブラリ では、音声は サンプリングレート=44100 チャンネル数=2 量子化ビット数=16bit にする必要あり */
object AkariCoreAudioProperties {

    /** チャンネル数 */
    const val CHANNEL_COUNT = 2

    /** サンプリングレート */
    const val SAMPLING_RATE = 44_100

    /** 量子化ビット数。16bit なので 2byte */
    const val BIT_DEPTH = 2

    /** 1秒間に必要な PCM のデータサイズ（バイト） */
    const val ONE_SECOND_PCM_DATA_SIZE = SAMPLING_RATE * CHANNEL_COUNT * BIT_DEPTH
}