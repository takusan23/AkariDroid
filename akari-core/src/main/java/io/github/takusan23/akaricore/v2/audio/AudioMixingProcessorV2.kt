package io.github.takusan23.akaricore.v2.audio

import io.github.takusan23.akaricore.v2.common.AkariCoreInputOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

object AudioMixingProcessorV2 {

    /**
     * 複数の PCM 音声を [ByteArray] で受け取って、合成した一つの PCM 音声にする。
     * サンプリングレート等、[AkariCoreAudioProperties]にあわせておく必要があります。
     *
     * @param output 出力先
     * @param durationMs 合計の長さ
     * @param onMixingByteArrays 合成したい音声ファイルの PCM データ（ByteArray）の配列を返してください。必要な回数分呼ばれます。第1引数は時間、第2引数は ByteArray のサイズです。
     */
    suspend fun start(
        output: AkariCoreInputOutput.Output,
        durationMs: Long,
        onMixingByteArrays: (positionSec: Int, byteArraySize: Int) -> List<ByteArray>
    ) = withContext(Dispatchers.IO) {
        // 音声を合成する

        // 出力先ファイルの OutputStream
        output.outputStream().use { outputStream ->

            // 音を重ねていく
            // サンプリングレートの分だけ
            // TODO たぶん、合成の際に秒より小さい値は見ていない。切り捨てられてる
            val durationSec = (durationMs / 1000).toInt()
            repeat(durationSec) { sec ->

                // 合成対象の ByteArray たち
                // 毎秒取り出す
                val byteArrayList = onMixingByteArrays(sec, AkariCoreAudioProperties.ONE_SECOND_PCM_DATA_SIZE)

                // 結果をいれる
                // 音声の合成が終わった後に毎回書き込んでいると遅い？
                // 書き込み回数を減らせば早くなるかもしれないので
                val resultByteArray = ByteArray(AkariCoreAudioProperties.ONE_SECOND_PCM_DATA_SIZE)

                // ByteArray 読み出し位置
                var pcmReadPosition = 0
                // resultByteArray 書き込み位置
                var resultByteArrayWritePosition = 0

                repeat(AkariCoreAudioProperties.SAMPLING_RATE) {
                    // それぞれの配列から同じ位置のデータを取り出す
                    // 以下のように、同じ位置のバイトを各ファイル取得して、全部足していく
                    // 最初の2バイトが多分左から聞こえて、次の2バイトが右から聞こえるはず
                    //
                    // ByteArray 0x00 0x00 0x00 0x00 ...
                    // ByteArray 0x01 0x01 0x01 0x01 ...
                    // ByteArray 0x02 0x02 0x02 0x02 ...
                    // ---------------------------------
                    // Result    0x03 0x03 0x03 0x03 ...

                    var leftSum = 0
                    var rightSum = 0
                    byteArrayList.forEach { byteArray ->
                        leftSum += byteArrayOf(byteArray[pcmReadPosition], byteArray[pcmReadPosition + 1]).toShort().toInt()
                    }
                    byteArrayList.forEach { byteArray ->
                        rightSum += byteArrayOf(byteArray[pcmReadPosition + 2], byteArray[pcmReadPosition + 3]).toShort().toInt()
                    }

                    // 合成する
                    // ただし 16bit を超えないように
                    val leftByteArray = min(Short.MAX_VALUE.toInt(), leftSum).toShort().toByteArray()
                    val rightByteArray = min(Short.MAX_VALUE.toInt(), rightSum).toShort().toByteArray()

                    resultByteArray[resultByteArrayWritePosition++] = leftByteArray[0]
                    resultByteArray[resultByteArrayWritePosition++] = leftByteArray[1]
                    resultByteArray[resultByteArrayWritePosition++] = rightByteArray[0]
                    resultByteArray[resultByteArrayWritePosition++] = rightByteArray[1]

                    // 2バイト、2チャンネル分読み出したので次に行く
                    pcmReadPosition += AkariCoreAudioProperties.BIT_DEPTH * AkariCoreAudioProperties.CHANNEL_COUNT
                }

                // 1秒間のデータが出来てからまとめて書き込む
                outputStream.write(resultByteArray)
            }
        }
    }
}