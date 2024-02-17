package io.github.takusan23.akaridroid.v2.audiorender

import android.content.Context
import io.github.takusan23.akaricore.v2.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.v2.audio.AudioMixingProcessor
import io.github.takusan23.akaridroid.v2.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * 音声を合成して PCM を返す
 *
 * @param pcmFolder デコードした PCM データの保存先
 * @param outPcmFile PCM 保存先
 * @param tempFolder 一時的な保存先
 */
class AudioRender(
    private val context: Context,
    private val pcmFolder: File,
    private val outPcmFile: File,
    private val tempFolder: File
) {

    /** 素材一覧 */
    private var audioItemRenderList: List<AudioItemRender> = emptyList()

    /** PCM をファイルから取り出すための[InputStream] */
    private var inputStream: InputStream? = null

    /** [audioRenderItem]をセットして、合成済みの PCM を作る */
    suspend fun setRenderData(audioRenderItem: List<RenderData.AudioItem>, durationMs: Long) = withContext(Dispatchers.IO) {
        // PCM 音声を合成する
        outPcmFile.delete()

        // 処理をスキップできるか。前回と素材が変化していないとき
        var isAllSkippable = true

        // 用意
        // 素材をデコードする
        try {
            tempFolder.mkdir()
            audioItemRenderList = audioRenderItem.filterIsInstance<RenderData.AudioItem.Audio>().map { audioItem ->
                // 並列で、デコーダー足りるかな
                async {
                    // すでにあれば何もしない
                    val exitsOrNull = audioItemRenderList.firstOrNull { it.isEquals(audioItem) }
                    if (exitsOrNull != null) {
                        return@async exitsOrNull
                    }

                    // ない場合
                    isAllSkippable = false
                    val newItem = AudioItemRender(
                        context = context,
                        audioItem = audioItem,
                        outPcmFile = createOutPcmFile(audioItem.id)
                    )
                    // デコードもしておく
                    // 一応フォルダも分けておく
                    val childTempFolder = tempFolder.resolve(audioItem.id.toString()).apply { mkdir() }
                    newItem.decode(childTempFolder)
                    return@async newItem
                }
            }.awaitAll()
        } finally {
            tempFolder.deleteRecursively()
        }

        // スキップ出来る場合はこれ以降何もしない
        if (isAllSkippable) return@withContext

        // 合成する際のパラメータ
        val mixList = audioItemRenderList.map { itemRender ->
            AudioMixingProcessor.MixAudioData(
                inPcmFile = itemRender.outPcmFile,
                startMs = itemRender.displayTime.startMs,
                stopMs = itemRender.displayTime.stopMs
            )
        }
        // 合成する
        AudioMixingProcessor.start(
            outPcmFile = outPcmFile,
            durationMs = durationMs,
            mixList = mixList
        )
        inputStream = outPcmFile.inputStream()
    }

    /** シークする。秒なので音ズレが訪れする */
    suspend fun seek(currentSec: Int) = withContext(Dispatchers.IO) {
        // InputStream なければ何もしない
        val inputStream = inputStream ?: return@withContext

        // 読み取り位置を見て、もし戻る必要があれば
        val currentReadPos = outPcmFile.length() - inputStream.available()
        val seekBytePos = (AkariCoreAudioProperties.CHANNEL_COUNT * AkariCoreAudioProperties.BIT_DEPTH * AkariCoreAudioProperties.SAMPLING_RATE) * currentSec
        if (currentReadPos < seekBytePos) {
            // Skip する
            inputStream.skip(seekBytePos - currentReadPos)
        } else {
            // 戻ってやり直す。その後 skip
            // InputStream は逆方向には skip 出来ない
            inputStream.reset()
            inputStream.skip(seekBytePos.toLong())
        }
    }

    /**
     * データを取り出す
     *
     * @param byteArray データの格納先
     */
    suspend fun readPcmByteArray(byteArray: ByteArray) = withContext(Dispatchers.IO) {
        inputStream?.read(byteArray)
    }

    /** 破棄する。生成したファイルを消す */
    fun destroy() {
        inputStream?.close()
        inputStream = null
        outPcmFile.delete()
        pcmFolder.deleteRecursively()
        tempFolder.deleteRecursively()
    }

    private fun createOutPcmFile(id: Long): File {
        return pcmFolder.resolve("$DECODE_PCM_FILE_PREFIX$id")
    }

    companion object {

        private const val DECODE_PCM_FILE_PREFIX = "pcm_file_"

    }

}