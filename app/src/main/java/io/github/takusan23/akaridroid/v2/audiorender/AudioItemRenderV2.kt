package io.github.takusan23.akaridroid.v2.audiorender

import android.content.Context
import android.media.MediaFormat
import androidx.core.net.toUri
import io.github.takusan23.akaricore.v2.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.v2.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.v2.audio.AudioVolumeProcessor
import io.github.takusan23.akaricore.v2.audio.ReSamplingRateProcessor
import io.github.takusan23.akaricore.v2.common.toAkariCoreInputDataSource
import io.github.takusan23.akaridroid.v2.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

// TODO Interface に切り出す
class AudioItemRenderV2(
    private val context: Context,
    private val audioItem: RenderData.AudioItem.Audio,
    private val outPcmFile: File,
) {

    private var inputStream: FileInputStream? = null

    private val decodeMutex = Mutex()
    private var isDecodeComplete = false

    val displayTime: RenderData.DisplayTime
        get() = audioItem.displayTime

    /** [audioItem]のファイルをデコードして加工できるようにする */
    suspend fun decode(tempFolder: File) = decodeMutex.withLock {
        // デコードが多重で動かないように mutex
        if (isDecodeComplete) return@withLock

        /** 一時的なファイルを作る */
        fun createTempFile(fileName: String): File = tempFolder
            .resolve(fileName)
            .apply { createNewFile() }

        delete()

        // InputStream を開く
        // デコーダーにかける
        var decoderMediaFormat: MediaFormat? = null
        val decodeFile = createTempFile(AUDIO_DECODE_FILE)
        AudioEncodeDecodeProcessor.decode(
            inAudioData = when (audioItem.filePath) {
                is RenderData.FilePath.File -> File(audioItem.filePath.filePath).toAkariCoreInputDataSource()
                is RenderData.FilePath.Uri -> audioItem.filePath.uriPath.toUri().toAkariCoreInputDataSource(context)
            },
            outPcmFile = decodeFile,
            onOutputFormat = { decoderMediaFormat = it }
        )

        // サンプリングレート変換が必要な場合
        // TODO チャンネル数は？
        val samplingRate = decoderMediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val fixSamplingRateDecodeFile = if (samplingRate != AkariCoreAudioProperties.SAMPLING_RATE) {
            createTempFile(AUDIO_FIX_SAMPLING).also { outFile ->
                ReSamplingRateProcessor.reSamplingBySonic(
                    inputDataSource = decodeFile.toAkariCoreInputDataSource(),
                    outPcmFile = outFile,
                    channelCount = AkariCoreAudioProperties.CHANNEL_COUNT,
                    inSamplingRate = samplingRate,
                    outSamplingRate = AkariCoreAudioProperties.SAMPLING_RATE
                )
            }
        } else {
            // 不要
            decodeFile
        }

        // 入れ直して終了
        // 音声調整と、指定範囲切り抜きは別にデコードのやり直しが必要じゃないので！
        fixSamplingRateDecodeFile.renameTo(outPcmFile)
    }

    /** [readPcmData]の前に呼び出す */
    suspend fun prepareRead() = withContext(Dispatchers.IO) {
        // InputStream を開く
        inputStream?.close()
        inputStream = outPcmFile.inputStream()
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
     * PCM データを取り出して ByteArray にいれる
     * 多分1秒間のデータで埋めることになる
     *
     * @param currentPositionSec 必要な再生位置。秒。戻ることはないはず。
     * @param byteArray [ByteArray]
     */
    suspend fun readPcmData(currentPositionSec: Long, byteArray: ByteArray) = withContext(Dispatchers.IO) {
        val inputStream = inputStream ?: return@withContext false
        // データを埋める
        val size = inputStream.read(byteArray)
        // 音量調整を適用する
        // TODO output に bytearray
        AudioVolumeProcessor.start(byteArray.toAkariCoreInputDataSource())
    }

    /** デコードのやり直し（PCM の作り直し）が必要かどうか */
    fun isRequireReDecode(item: RenderData.AudioItem): Boolean {
        // 今のところ Audio 以外は無い
        if (item !is RenderData.AudioItem.Audio) {
            return true
        }
        // ファイルパスとサンプリングレート変換をやっているので、それ以外が変わったらやり直しが必要
        // が、サンプリングレートはファイルが変わらない場合はスキップできるので
        return item.filePath != audioItem.filePath
    }

    /** データが同じかどうかを返す */
    fun isEquals(item: RenderData.AudioItem): Boolean {
        return audioItem == item
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

    /** デコードした PCM ファイルを消す。もう素材として利用しないとき用 */
    suspend fun delete() = withContext(Dispatchers.IO) {
        inputStream?.close()
        outPcmFile.delete()
    }

    companion object {
        private const val AUDIO_COPY_FROM_URI = "audio_copy_from_uri"
        private const val AUDIO_CROP_FILE = "audio_crop_file"
        private const val AUDIO_DECODE_FILE = "audio_decode_file"
        private const val AUDIO_FIX_VOLUME = "audio_fix_volume"
        private const val AUDIO_FIX_SAMPLING = "audio_fix_sampling"
    }
}