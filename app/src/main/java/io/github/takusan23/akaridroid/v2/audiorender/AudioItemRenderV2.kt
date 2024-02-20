package io.github.takusan23.akaridroid.v2.audiorender

import io.github.takusan23.akaricore.v2.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.v2.audio.AudioVolumeProcessor
import io.github.takusan23.akaricore.v2.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.v2.common.toAkariCoreInputOutputData
import io.github.takusan23.akaridroid.v2.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

// TODO Interface に切り出す

/**
 * [AudioDecodeManager]でデコードしたファイルを[RenderData.AudioItem.Audio]通りに音声加工するクラス
 *
 * @param audioItem 音声素材の情報
 * @param decodePcmFile デコードした PCM ファイル
 */
class AudioItemRenderV2(
    private val audioItem: RenderData.AudioItem.Audio,
    private val decodePcmFile: File
) {
    private var inputStream: FileInputStream? = null

    /** [readPcmData]の前に呼び出す */
    suspend fun prepareRead() = withContext(Dispatchers.IO) {
        // InputStream を開く
        inputStream?.close()
        inputStream = decodePcmFile.inputStream()
        // 音声ファイルをカットする場合
        // 読み出し開始位置を skip して調整しておく
        if (audioItem.cropTime != null) {
            // 秒にする
            // TODO ミリ秒単位の調整には対応していない
            val startSec = audioItem.cropTime.cropStartMs / 1_000
            val skipBytes = startSec * AkariCoreAudioProperties.ONE_SECOND_PCM_DATA_SIZE
            inputStream?.skip(skipBytes)
        }
    }

    /**
     * PCM データを取り出して ByteArray にいれる。
     * 多分1秒間のデータで埋めることになる。
     * 音量調整とかはここで適用される。
     *
     * @param readSize 次読み出すデータのサイズ
     * @return 読み出し結果[ByteArray]
     */
    suspend fun readPcmData(readSize: Int) = withContext(Dispatchers.IO) {
        val inputStream = inputStream ?: return@withContext byteArrayOf()

        // データを埋める
        var readByteArray = ByteArray(readSize)
        inputStream.read(readByteArray)

        // 音量調整を適用する
        // 加工したデータはメモリに乗せるので
        if (audioItem.volume != RenderData.AudioItem.DEFAULT_VOLUME) {
            val output = AkariCoreInputOutput.OutputJavaByteArray()
            AudioVolumeProcessor.start(
                input = readByteArray.toAkariCoreInputOutputData(),
                output = output,
                volume = audioItem.volume
            )
            readByteArray = output.byteArray
        }

        readByteArray
    }

    fun isDisplayPosition(currentPositionMs: Long): Boolean {
        // 範囲内にいること
        if (currentPositionMs !in audioItem.displayTime) {
            return false
        }
        val framePositionMs = currentPositionMs - audioItem.displayTime.startMs

        // 動画をカットする場合で、カットした時間外の場合
        if (audioItem.cropTime != null && framePositionMs !in audioItem.cropTime) {
            return false
        }
        return true
    }

    /** 破棄する */
    fun destroy() {
        inputStream?.close()
    }
}