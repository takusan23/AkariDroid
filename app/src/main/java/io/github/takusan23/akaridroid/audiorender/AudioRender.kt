package io.github.takusan23.akaridroid.audiorender

import android.content.Context
import io.github.takusan23.akaricore.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.audio.AudioMixingProcessor
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * 音声を合成して PCM を返す
 *
 * @param context Uri を扱う場合に
 * @param outPcmFile 合成済みの PCM データ保存先
 * @param outputDecodePcmFolder デコードした PCM データの保存先
 * @param tempFolder 一時的な保存先
 */
class AudioRender(
    context: Context,
    val outPcmFile: File,
    private val outputDecodePcmFolder: File,
    private val tempFolder: File
) {

    /** 素材をデコードしてくれるやつ */
    private val audioDecodeManager = AudioDecodeManager(
        context = context,
        tempFolder = tempFolder,
        outputDecodePcmFolder = outputDecodePcmFolder
    )

    /** デコード済みで使える素材一覧 */
    private var audioItemRenderList: List<AudioItemRender> = emptyList()

    /** PCM をファイルから取り出すための[InputStream] */
    private var inputStream: InputStream? = null

    /**
     * [audioRenderItem]をセットして、合成済みの PCM を作る。
     * デコードが終わるまで一時停止しますが、キャンセル可能です。キャンセルした場合でもデコードは続行します（別のコルーチンスコープなので）。
     */
    suspend fun setRenderData(
        audioRenderItem: List<RenderData.AudioItem>,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        // TODO 今のところ Audio のみしか来ない
        val audioList = audioRenderItem.filterIsInstance<RenderData.AudioItem.Audio>()
        val filePathList = audioList.map { it.filePath }

        // 前回から削除された素材はデコード中ならキャンセルさせ、PCM ファイルも消す
        audioDecodeManager
            .addedDecoderFilePathList
            .forEach { oldItem ->
                // 前回から削除された素材を消す
                if (oldItem !in filePathList) {
                    audioDecodeManager.cancelDecodeAndDeleteFile(oldItem)
                }
            }

        // デコーダーに入れてデコードさせる
        audioRenderItem
            .filterIsInstance<RenderData.AudioItem.Audio>()
            .forEach { audioItem ->
                val id = audioItem.id
                val filePath = audioItem.filePath
                // 前回からの追加分の場合は追加
                if (!audioDecodeManager.hasDecode(filePath)) {
                    audioDecodeManager.addDecode(filePath, "$DECODE_PCM_FILE_PREFIX$id")
                }
            }

        // デコードが終わるのを待つ
        // もしデコード中に素材が変わった場合はキャンセルしてね、この待機もキャンセルされます
        audioDecodeManager.awaitAllDecode()

        // AudioItemRender を作る
        audioItemRenderList.forEach { it.destroy() }
        audioItemRenderList = audioList.mapNotNull { audioItem ->
            audioDecodeManager.getDecodedPcmFile(audioItem.filePath)?.let { decodedFile ->
                AudioItemRender(
                    audioItem = audioItem,
                    decodePcmFile = decodedFile
                )
            }
        }

        // 合成前に呼び出しておく
        audioItemRenderList.forEach { it.prepareRead() }

        // 音声素材をを合成する
        AudioMixingProcessor.start(
            output = outPcmFile.toAkariCoreInputOutputData(),
            durationMs = durationMs,
            onMixingByteArrays = { positionMs, byteArraySize ->
                // 範囲内のものを取り出す
                audioItemRenderList
                    .filter { it.isDisplayPosition(positionMs) }
                    .map { it.readPcmData(byteArraySize) }
            }
        )

        // 音声ファイル出来たら InputStream を開く
        inputStream = outPcmFile.inputStream()
    }

    /**
     * シークする
     *
     * @param currentPositionMs シークしたい位置。ミリ秒
     */
    suspend fun seek(currentPositionMs: Long) = withContext(Dispatchers.IO) {
        // 読み取り位置を見て、もし戻る必要があれば
        val available = inputStream?.available() ?: return@withContext
        val currentReadPos = outPcmFile.length() - available
        val seekBytePos = AkariCoreAudioProperties.ONE_MILLI_SECONDS_PCM_DATA_SIZE * currentPositionMs
        if (currentReadPos < seekBytePos) {
            // Skip する
            inputStream?.skip(seekBytePos - currentReadPos)
        } else {
            // 戻れないので、InputStream を開き直す
            inputStream?.close()
            inputStream = outPcmFile.inputStream()
            inputStream?.skip(seekBytePos.toLong())
        }
    }

    /**
     * データを取り出す
     *
     * @param byteArray データの格納先
     */
    suspend fun readPcmByteArray(byteArray: ByteArray) = withContext(Dispatchers.IO) {
        if (inputStream?.available() != 0) {
            inputStream?.read(byteArray)
        } else 0
    }

    /** 破棄する。生成したファイルを消す */
    fun destroy() {
        inputStream?.close()
        inputStream = null
        audioDecodeManager.destroy()
        audioItemRenderList.forEach { it.destroy() }

        outPcmFile.delete()
        outputDecodePcmFolder.deleteRecursively()
        tempFolder.deleteRecursively()
    }

    companion object {
        private const val DECODE_PCM_FILE_PREFIX = "pcm_file_"
    }

}