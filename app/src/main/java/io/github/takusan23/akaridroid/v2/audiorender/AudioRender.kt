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
        inputStream?.close()
        inputStream = null

        // 用意
        // 素材をデコードする
        try {
            tempFolder.mkdir()
            val newAudioItemRenderList = audioRenderItem.filterIsInstance<RenderData.AudioItem.Audio>().map { audioItem ->
                // 並列で、デコーダー足りるかな
                async {
                    // すでにあれば何もしない
                    // TODO デコードとサンプリングレート変換だけやって、切り抜きとかは Mix のときにやると良さそう。
                    val exitsOrNull = audioItemRenderList.firstOrNull { it.isEquals(audioItem) }
                    if (exitsOrNull != null) {
                        return@async exitsOrNull
                    }

                    // ない場合
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

            // 前回から要らなくなったものを削除する
            // 前回と同じなら isEquals が true のはず。true ならそのまま残す
            // false で前回までしか残ってないやつは破棄する
            // TODO が、、、なんか消しちゃいけないものまで消してる
            /*
                        audioItemRenderList.forEach { oldRender ->
                            // 含まれていなければ使われてない
                            if (audioRenderItem.none { new -> oldRender.isEquals(new) }) {
                                oldRender.outPcmFile.delete()
                            }
                        }
            */

            audioItemRenderList = newAudioItemRenderList

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
            // InputStream を開く
            inputStream = outPcmFile.inputStream()
        } finally {
            // 終了時 tempFolder 削除
            tempFolder.deleteRecursively()
        }
    }

    /** シークする。秒なので音ズレが訪れする */
    suspend fun seek(currentSec: Int) = withContext(Dispatchers.IO) {
        // 読み取り位置を見て、もし戻る必要があれば
        val available = inputStream?.available() ?: return@withContext
        val currentReadPos = outPcmFile.length() - available
        val seekBytePos = (AkariCoreAudioProperties.CHANNEL_COUNT * AkariCoreAudioProperties.BIT_DEPTH * AkariCoreAudioProperties.SAMPLING_RATE) * currentSec
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