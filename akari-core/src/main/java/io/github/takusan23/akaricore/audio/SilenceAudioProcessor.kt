package io.github.takusan23.akaricore.audio

import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 無音の音声ファイルを作成する */
object SilenceAudioProcessor {

    /**
     * 音声を生成する
     *
     * @param output PCM 出力先
     * @param durationMs 音声の時間（ミリ秒）
     * @param samplingRate サンプリングレート。大体 44100
     * @param channelCount チャンネル数。ステレオなら 2
     * @param bitDepth 量子化ビット数。単位は byte。ほぼ 16bit だろうし 2
     */
    suspend fun start(
        output: AkariCoreInputOutput.Output,
        durationMs: Long,
        samplingRate: Int = 44_100,
        channelCount: Int = 2,
        bitDepth: Int = 2
    ) = withContext(Dispatchers.Default) {
        // 空の音声を用意する
        // 大抵の動画ファイルの音声の量子化ビット数は 16bit なので、2 バイト使う
        // ステレオなので 2 チャンネル
        // それをサンプリングレートでかけると、1秒間に必要なバイトサイズが出る
        // そして最後に秒をかけて、無音のPCMデータを作成する
        // TODO メモリにPCMデータを載せておくのはもったいないことしてるかもしれない
        val second = (durationMs / 1_000f).toInt()
        val pcmByteSize = channelCount * bitDepth * samplingRate
        output.outputStream().use { outputStream ->
            repeat(second) {
                outputStream.write(ByteArray(pcmByteSize))
            }
        }
    }

}