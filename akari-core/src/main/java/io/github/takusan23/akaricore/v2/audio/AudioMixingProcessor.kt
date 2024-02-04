package io.github.takusan23.akaricore.v2.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.min

/**
 * 音声をエンコードする。
 * 複数ファイルを指定すると音声を重ねてエンコードする。
 *
 * 音の合成ですが、難しいことはなく、波の音を重ねることで音がミックスされます。
 * 実装的には、AAC を PCM にして（ Audacity の Raw データのように）、バイト配列から同じ位置の Byte を足すことでできます。
 */
object AudioMixingProcessor {

    /**
     * 音声 PCM を合成する
     *
     * @param outPcmFile 合成したファイルの出力先
     * @param mixList 合成したい PCM ファイル
     * @param durationMs 合計の長さ
     */
    suspend fun start(
        outPcmFile: File,
        durationMs: Long,
        mixList: List<MixAudioData>
    ) = withContext(Dispatchers.IO) {
        // 出力先ファイルの OutputStream
        val outputStream = outPcmFile.outputStream()
        // InputStream を作る
        val mixAudioStreamDataList = mixList.map {
            MixAudioStreamData(it, it.inPcmFile.inputStream())
        }

        // 音声を合成する
        // 以下のように、同じ位置のバイトを各ファイル取得して、全部足していく
        // File-1 0x00 0x00 0x00 ...
        // File-2 0x01 0x01 0x01 ...
        // File-3 0x02 0x02 0x02 ...
        // -------------------------
        // Result 0x03 0x03 0x03 ...

        // 音を重ねていく
        // サンプリングレートの分だけ
        val durationSec = (durationMs / 1000).toInt()
        repeat(durationSec) { sec ->

            // 同時に再生する音を取得
            val inputStreamList = mixAudioStreamDataList
                .filter { (sec * 1000) in it.mixAudioData.playPositionMs }

            repeat(AkariCoreAudioProperties.SAMPLING_RATE) {
                // サンプリングレートの回数分呼ばれる
                // 音を作ります
                // 2 チャンネル、量子化ビット数 16bit なので、2 + 2 = 4 byte 必要です
                if (inputStreamList.isEmpty()) {
                    // 無い場合は、無音を書き込む
                    val left = ByteArray(AkariCoreAudioProperties.BIT_DEPTH)
                    val right = ByteArray(AkariCoreAudioProperties.BIT_DEPTH)
                    outputStream.write(left)
                    outputStream.write(right)
                } else {
                    // 音がある場合、しかも複数ある場合は足す
                    // 音は波らしい。波は足し算できる
                    val (leftList, rightList) = inputStreamList.map { (mixAudioData, inputStream) ->
                        // 右と左の音を取り出す
                        val leftByteArray = ByteArray(AkariCoreAudioProperties.BIT_DEPTH)
                        inputStream.read(leftByteArray)
                        val rightByteArray = ByteArray(AkariCoreAudioProperties.BIT_DEPTH)
                        inputStream.read(rightByteArray)

                        // ボリューム調整のために Int にする
                        val leftInt = leftByteArray.toShort().toInt()
                        val rightInt = rightByteArray.toShort().toInt()
                        // map の返り値
                        leftInt to rightInt
                    }.let { readAudioList ->
                        // 右と左でそれぞれ配列を分ける
                        readAudioList.map { it.first } to readAudioList.map { it.second }
                    }

                    // 合成する
                    // ただし 16bit を超えないように
                    val sumLeftByteArray = min(Short.MAX_VALUE.toInt(), leftList.sum()).toShort().toByteArray()
                    val sumRightByteArray = min(Short.MAX_VALUE.toInt(), rightList.sum()).toShort().toByteArray()
                    // ByteArray に戻して、書き込む
                    outputStream.write(sumLeftByteArray)
                    outputStream.write(sumRightByteArray)
                }
            }
        }

        // リソース開放
        mixAudioStreamDataList.forEach { it.inputStream.close() }
        outputStream.close()
    }

    /**
     * 音声を合成する際に必要な値
     * PCM ファイルは、サンプリングレート 44100 、2 チャンネル、量子化ビット数 16bit である必要があります。
     *
     * @param inPcmFile PCM ファイルのパス
     * @param startMs 合成する再生開始位置
     */
    data class MixAudioData(
        val inPcmFile: File,
        val startMs: Long
    ) {

        /** 音声ファイルの長さ（秒） */
        val durationSec: Long
            get() = inPcmFile.length() / (AkariCoreAudioProperties.SAMPLING_RATE * AkariCoreAudioProperties.CHANNEL_COUNT * AkariCoreAudioProperties.BIT_DEPTH)

        /** 再生すべき位置の範囲を返す。開始位置から、開始位置 + ファイルの長さ */
        val playPositionMs: LongRange
            get() = startMs..(startMs + (durationSec * 1000))

    }

    /** 内部で使う */
    private data class MixAudioStreamData(
        val mixAudioData: MixAudioData,
        val inputStream: InputStream
    )
}