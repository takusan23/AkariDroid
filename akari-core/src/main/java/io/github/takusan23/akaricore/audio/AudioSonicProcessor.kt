package io.github.takusan23.akaricore.audio

import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/** [Sonic]ライブラリを利用した PCM 音声に対する操作 */
object AudioSonicProcessor {

    private const val BUFFER_SIZE = 8192

    /**
     * [Sonic]ライブラリを使ってアップサンプリングする
     *
     * @param input 変換前 PCM ファイル
     * @param output 変換後 PCM ファイル
     * @param channelCount チャンネル数
     * @param inSamplingRate 変換前のサンプリングレート
     * @param outSamplingRate 変換後のサンプリングレート
     */
    suspend fun reSamplingBySonic(
        input: AkariCoreInputOutput.Input,
        output: AkariCoreInputOutput.Output,
        channelCount: Int,
        inSamplingRate: Int,
        outSamplingRate: Int
    ) = withContext(Dispatchers.IO) {

        // Sonic を利用してアップサンプリングを行う
        val sonic = Sonic(inSamplingRate, channelCount)
        sonic.sampleRate = outSamplingRate
        sonic.rate = inSamplingRate.toFloat() / outSamplingRate.toFloat()

        startSonic(
            sonic = sonic,
            inputStream = input.inputStream(),
            outputStream = output.outputStream()
        )
    }

    /**
     * [Sonic]ライブラリを使って再生速度の調整を行う
     *
     * @param input 変換前 PCM ファイル
     * @param output 変換後 PCM ファイル
     * @param samplingRate サンプリングレート
     * @param channelCount チャンネル数
     * @param speed 速度。1f でデフォルト
     */
    suspend fun playbackSpeedBySonic(
        input: AkariCoreInputOutput.Input,
        output: AkariCoreInputOutput.Output,
        samplingRate: Int,
        channelCount: Int,
        speed: Float
    ) = withContext(Dispatchers.IO) {
        val sonic = Sonic(samplingRate, channelCount)
        sonic.speed = speed

        startSonic(
            sonic = sonic,
            inputStream = input.inputStream(),
            outputStream = output.outputStream()
        )
    }

    /**
     * [Sonic]を利用して、PCM 音声に操作を行う
     *
     * @param sonic [Sonic]
     * @param inputStream PCM 入力元
     * @param outputStream PCM 出力先
     */
    private suspend fun startSonic(
        sonic: Sonic,
        inputStream: InputStream,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val inByteArray = ByteArray(BUFFER_SIZE)
        val outByteArray = ByteArray(BUFFER_SIZE)
        var numRead: Int
        var numWritten: Int

        inputStream.buffered().use { inputStream ->
            outputStream.buffered().use { outputStream ->
                do {
                    // キャンセルチェック
                    if (!isActive) break

                    numRead = inputStream.read(inByteArray, 0, BUFFER_SIZE)
                    if (numRead <= 0) {
                        sonic.flushStream()
                    } else {
                        sonic.writeBytesToStream(inByteArray, numRead)
                    }
                    do {
                        // キャンセルチェック
                        if (!isActive) break

                        numWritten = sonic.readBytesFromStream(outByteArray, BUFFER_SIZE)
                        if (numWritten > 0) {
                            outputStream.write(outByteArray, 0, numWritten)
                        }
                    } while (numWritten > 0)
                } while (numRead > 0)
            }
        }
    }

}