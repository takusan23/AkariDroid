package io.github.takusan23.akaridroid.v2.audiorender

import android.content.Context
import io.github.takusan23.akaridroid.v2.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

// TODO Interface に切り出す
class AudioItemRenderV2(
    private val context: Context,
    private val audioItem: RenderData.AudioItem.Audio,
    private val outPcmFile: File,
) {

    private val decodeMutex = Mutex()
    private var isDecodeComplete = false

    /** [audioItem]のファイルをデコードして加工できるようにする */
    suspend fun decode(tempFolder: File) = decodeMutex.withLock {
        // デコードが多重で動かないように mutex
        if (isDecodeComplete) return@withLock

        /** 一時的なファイルを作る */
        fun createTempFile(fileName: String): File = tempFolder
            .resolve(fileName)
            .apply { createNewFile() }

        // InputStream を開く
        when (audioItem.filePath) {
            is RenderData.FilePath.File -> TODO()
            is RenderData.FilePath.Uri -> TODO()
        }

    }

    /** [byteArray]を渡すと、それにPCMデータを詰め込んで返します */
    suspend fun getSample(currentPositionMs: Long, byteArray: ByteArray) = withContext(Dispatchers.IO) {
        // TODO カット処理
        // TODO 音量調整
    }

    /** デコードした PCM ファイルを消す。もう素材として利用しないとき用 */
    suspend fun delete() = withContext(Dispatchers.IO) {
        outPcmFile.delete()
    }

    companion object {
        private const val AUDIO_COPY_FROM_URI = "audio_copy_from_uri"
        private const val AUDIO_CROP_FILE = "audio_crop_file"
        private const val AUDIO_DECODE_FILE = "audio_decode_file"
        private const val AUDIO_FIX_VOLUME = "audio_fix_volume"
        private const val AUDIO_FIX_SAMPLIN = "audio_fix_sampling"
    }
}