package io.github.takusan23.akaricore.v2.audio

import io.github.takusan23.akaricore.v2.common.AkariCoreInputDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** [Sonic]を利用した、サンプリングレート変換器 */
object ReSamplingRateProcessor {
    private const val BUFFER_SIZE = 8192

    /**
     * [Sonic]ライブラリを使ってアップサンプリングする
     *
     * @param inputDataSource 変換前 PCM ファイルを表す[AkariCoreInputDataSource]
     * @param outPcmFile 変換後 PCM ファイル
     * @param channelCount チャンネル数
     * @param inSamplingRate 変換前のサンプリングレート
     * @param outSamplingRate 変換後のサンプリングレート
     */
    suspend fun reSamplingBySonic(
        inputDataSource: AkariCoreInputDataSource,
        outPcmFile: File,
        channelCount: Int,
        inSamplingRate: Int,
        outSamplingRate: Int
    ) = withContext(Dispatchers.Default) {
        val inByteArray = ByteArray(BUFFER_SIZE)
        val outByteArray = ByteArray(BUFFER_SIZE)
        var numRead: Int
        var numWritten: Int

        // Sonic を利用してアップサンプリングを行う
        val sonic = Sonic(inSamplingRate, channelCount)
        sonic.sampleRate = outSamplingRate
        sonic.speed = 1f
        sonic.pitch = 1f
        sonic.rate = inSamplingRate.toFloat() / outSamplingRate.toFloat()
        sonic.volume = 1f
        sonic.chordPitch = false
        sonic.quality = 0

        inputDataSource.inputStream().use { inputStream ->
            outPcmFile.outputStream().use { outputStream ->

                do {
                    numRead = inputStream.read(inByteArray, 0, BUFFER_SIZE)
                    if (numRead <= 0) {
                        sonic.flushStream()
                    } else {
                        sonic.writeBytesToStream(inByteArray, numRead)
                    }
                    do {
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