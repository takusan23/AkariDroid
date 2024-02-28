package io.github.takusan23.akaricore.audio

import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * モノラル音声（チャンネル数 1）をステレオ音声（チャンネル数 2）にする。
 * チャンネル数は[AkariCoreAudioProperties.CHANNEL_COUNT]が期待値なので
 */
object AudioMonoToStereoProcessor {

    /**
     * モノラル音声（チャンネル数 1）の PCM ファイルをステレオ音声（チャンネル数 2）にする。
     *
     * 一回のサンプリングで、2バイトしか使わなかったのを、左で2バイト、右で2バイトになるように増やす。
     * 無理やりステレオにするので、右も左も同じ音しかでません。
     *
     * @param input 入力 PCM
     * @param output 出力 PCM
     */
    suspend fun start(
        input: AkariCoreInputOutput.Input,
        output: AkariCoreInputOutput.Output
    ) = withContext(Dispatchers.IO) {
        val monoByteArray = ByteArray(AkariCoreAudioProperties.BIT_DEPTH)

        input.inputStream().buffered().use { inputStream ->
            output.outputStream().buffered().use { outputStream ->
                while (isActive) {
                    if (inputStream.available() == 0) break
                    // 取り出して、右と左から出るように増やす
                    inputStream.read(monoByteArray)
                    outputStream.write(monoByteArray)
                    outputStream.write(monoByteArray)
                }
            }
        }
    }
}