package io.github.takusan23.akaricore.audio

import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** PCM の音量調整 */
object AudioVolumeProcessor {

    /**
     * PCM ファイルの音量調整をする
     *
     * @param input 入力 PCM
     * @param output 出力 PCM
     * @param volume 音量
     */
    suspend fun start(
        input: AkariCoreInputOutput.Input,
        output: AkariCoreInputOutput.Output,
        volume: Float
    ) = withContext(Dispatchers.IO) {
        val leftByteArray = ByteArray(AkariCoreAudioProperties.BIT_DEPTH)
        val rightByteArray = ByteArray(AkariCoreAudioProperties.BIT_DEPTH)
        input.inputStream().buffered().use { inputStream ->
            output.outputStream().use { outputStream ->
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