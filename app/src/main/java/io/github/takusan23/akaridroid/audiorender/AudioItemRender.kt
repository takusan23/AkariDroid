package io.github.takusan23.akaridroid.audiorender

import io.github.takusan23.akaricore.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.audio.AudioVolumeProcessor
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * [AudioDecodeManager]でデコードしたファイルを[RenderData.AudioItem.Audio]通りに音声加工するクラス
 *
 * @param audioItem 音声素材の情報
 * @param decodePcmFile デコードした PCM ファイル
 */
class AudioItemRender(
    private val audioItem: RenderData.AudioItem.Audio,
    private val decodePcmFile: File
) : AudioRenderInterface {

    private var inputStream: FileInputStream? = null

    override suspend fun prepareRead() = withContext(Dispatchers.IO) {
        // InputStream を開く
        inputStream?.close()
        inputStream = decodePcmFile.inputStream()
        // 音声ファイルをカットする場合
        // 読み出し開始位置を skip して調整しておく
        if (audioItem.positionOffset != null) {
            // 秒にする
            // TODO ミリ秒単位の調整には対応していない
            val startSec = audioItem.positionOffset.offsetFirstMs / 1_000
            val skipBytes = startSec * AkariCoreAudioProperties.ONE_SECOND_PCM_DATA_SIZE
            inputStream?.skip(skipBytes)
        }
    }

    override suspend fun readPcmData(readSize: Int): ByteArray = withContext(Dispatchers.IO) {
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

    override fun isDisplayPosition(currentPositionMs: Long): Boolean = currentPositionMs in audioItem.displayTime

    /** 破棄する */
    override fun destroy() {
        inputStream?.close()
    }
}