package io.github.takusan23.akaricore.v2.audio

import android.media.MediaFormat
import android.media.MediaMuxer
import io.github.takusan23.akaricore.v1.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** [Sonic]を利用した、サンプリングレート変換器 */
object ReSamplingRateProcessor {

    /**
     * Sonic ライブラリを使ってアップサンプリングする
     *
     * @param inPcmFile 変換前 PCM ファイル
     * @param outPcmFile 変換後 PCM ファイル
     * @param channelCount チャンネル数
     * @param inSamplingRate 変換前のサンプリングレート
     * @param outSamplingRate 変換後のサンプリングレート
     */
    suspend fun reSamplingBySonic(
        inPcmFile: File,
        outPcmFile: File,
        channelCount: Int,
        inSamplingRate: Int,
        outSamplingRate: Int
    ) = withContext(Dispatchers.Default) {
        val bufferSize = 8192
        val inByteArray = ByteArray(bufferSize)
        val outByteArray = ByteArray(bufferSize)
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

        inPcmFile.inputStream().use { inputStream ->
            outPcmFile.outputStream().use { outputStream ->

                do {
                    numRead = inputStream.read(inByteArray, 0, bufferSize)
                    if (numRead <= 0) {
                        sonic.flushStream()
                    } else {
                        sonic.writeBytesToStream(inByteArray, numRead)
                    }
                    do {
                        numWritten = sonic.readBytesFromStream(outByteArray, bufferSize)
                        if (numWritten > 0) {
                            outputStream.write(outByteArray, 0, numWritten)
                        }
                    } while (numWritten > 0)
                } while (numRead > 0)
            }
        }
    }

}