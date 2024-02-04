package io.github.takusan23.akaricore.v2.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

/** PCM の音量調整 */
object AudioVolumeProcessor {

    /**
     * PCM ファイルの音量調整をする
     *
     * @param inPcmFile 入力 PCM
     * @param outPcmFile 出力 PCM
     * @param volume 音量
     */
    suspend fun start(
        inPcmFile: File,
        outPcmFile: File,
        volume: Float
    ) = withContext(Dispatchers.IO) {
        val leftByteArray = ByteArray(AkariCoreAudioProperties.BIT_DEPTH)
        val rightByteArray = ByteArray(AkariCoreAudioProperties.BIT_DEPTH)
        inPcmFile.inputStream().use { inputStream ->
            outPcmFile.outputStream().use { outputStream ->
                while (isActive) {
                    if (inputStream.available() == 0) break

                    // 2 チャンネルあるはず
                    inputStream.read(leftByteArray)
                    inputStream.read(rightByteArray)

                    // Int にして、音量調整をする
                    val applyVolumeLeftInt = (leftByteArray.toShort().toInt() * volume).toInt()
                    val applyVolumeRightInt = (rightByteArray.toShort().toInt() * volume).toInt()

                    // ByteArray にしてしまう
                    outputStream.write(applyVolumeLeftInt.toShort().toByteArray())
                    outputStream.write(applyVolumeRightInt.toShort().toByteArray())
                }
            }
        }
    }

}