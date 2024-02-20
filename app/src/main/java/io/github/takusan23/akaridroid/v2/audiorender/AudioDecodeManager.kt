package io.github.takusan23.akaridroid.v2.audiorender

import android.content.Context
import android.media.MediaFormat
import androidx.core.net.toUri
import io.github.takusan23.akaricore.v2.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.v2.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.v2.audio.ReSamplingRateProcessor
import io.github.takusan23.akaricore.v2.common.toAkariCoreInputOutputData
import io.github.takusan23.akaridroid.v2.RenderData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * [AudioRender]から PCM デコード関連のコードがここにある。
 * デコード開始、終了、デコード済み一覧等。
 * 複数の音声ファイルをそれぞれ PCM にデコードするやつ。
 *
 * [RenderData.AudioItem]ではなく、[RenderData.FilePath]をキーにしているのは同じファイルの[RenderData.AudioItem]ならスキップさせるため
 */
class AudioDecodeManager(
    private val context: Context,
    private val tempFolder: File,
    private val outputDecodePcmFolder: File
) {

    /** PCM デコード用コルーチンスコープ */
    private val decodeScope = CoroutineScope(Dispatchers.Default + Job())

    /** デコード中、デコード済みのファイルの配列[AudioDecodeItem] */
    private var decodeItemList = listOf<AudioDecodeItem>()

    /** 追加済みの[RenderData.FilePath] */
    val addedDecoderFilePathList: List<RenderData.FilePath>
        get() = decodeItemList.map { it.filePath }

    /** [addDecode]で追加済みかどうかを返す */
    fun hasDecode(filePath: RenderData.FilePath): Boolean {
        return filePath in addedDecoderFilePathList
    }

    /**
     * デコードするファイルを追加する。
     * コルーチンをキャンセルしても、デコード処理は別のコルーチンスコープなのでキャンセルされません。
     *
     * @param filePath 音声ファイル
     * @param outputFileName デコードした音声ファイル PCM の名前
     */
    fun addDecode(filePath: RenderData.FilePath, outputFileName: String) {
        val outputDecodePcmFile = outputDecodePcmFolder.resolve(outputFileName)
        // 非同期でデコードさせる
        val decodeJob = decodeScope.launch { decode(filePath, outputDecodePcmFile) }
        // 追加する
        decodeItemList = decodeItemList + AudioDecodeItem(filePath, outputDecodePcmFile, decodeJob)
    }

    /**
     * デコード中ならキャンセルして、ファイルも消す
     *
     * @param filePath 音声ファイル
     */
    suspend fun cancelDecodeAndDeleteFile(filePath: RenderData.FilePath) {
        // なければ return
        val exitsItem = getDecodeItemOrNull(filePath) ?: return
        // キャンセルしてデコードしたファイルも消す
        exitsItem.decodeJob.cancelAndJoin()
        exitsItem.outputDecodePcmFile.delete()
        decodeItemList = decodeItemList - exitsItem
    }

    /**
     * すべてのデコードを待つ
     * [addDecode]を呼び出した後にこれで待つ
     */
    suspend fun awaitAllDecode() {
        decodeItemList.map { it.decodeJob }.joinAll()
    }

    /**
     * デコード済みのファイルを取得する
     *
     * @param filePath 音声ファイル
     * @return デコード済みならファイル、ない場合、デコード終わってない場合は null
     */
    fun getDecodedPcmFile(filePath: RenderData.FilePath): File? {
        val decodeItem = getDecodeItemOrNull(filePath) ?: return null
        return if (decodeItem.decodeJob.isCompleted) {
            decodeItem.outputDecodePcmFile
        } else {
            null
        }
    }

    /**
     * 破棄する。
     * [outputDecodePcmFolder]は別途消してください（プレビューと動画書き出しで使い回す？）。
     * TODO 動画書き出し時に PCM ファイル使い回す？
     */
    fun destroy() {
        decodeScope.cancel()
        // outputDecodePcmFolder.delete()
    }

    /** [filePath]から[AudioDecodeItem]を探す */
    private fun getDecodeItemOrNull(filePath: RenderData.FilePath) = decodeItemList.firstOrNull { it.filePath == filePath }

    /**
     * デコードする
     *
     * @param filePath ファイルパス
     * @param outputDecodePcmFile デコードした PCM ファイル
     */
    private suspend fun decode(
        filePath: RenderData.FilePath,
        outputDecodePcmFile: File
    ) {
        val decodeFile = createTempFile(AUDIO_DECODE_FILE)
        val reSamplingFile = createTempFile(AUDIO_FIX_SAMPLING)

        try {
            // デコーダーにかける
            var decoderMediaFormat: MediaFormat? = null
            AudioEncodeDecodeProcessor.decode(
                input = when (filePath) {
                    is RenderData.FilePath.File -> File(filePath.filePath).toAkariCoreInputOutputData()
                    is RenderData.FilePath.Uri -> filePath.uriPath.toUri().toAkariCoreInputOutputData(context)
                },
                output = decodeFile.toAkariCoreInputOutputData(),
                onOutputFormat = { decoderMediaFormat = it }
            )

            // サンプリングレート変換が必要な場合
            // TODO チャンネル数は？
            val samplingRate = decoderMediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val fixSamplingRateDecodeFile = if (samplingRate != AkariCoreAudioProperties.SAMPLING_RATE) {
                ReSamplingRateProcessor.reSamplingBySonic(
                    input = decodeFile.toAkariCoreInputOutputData(),
                    output = reSamplingFile.toAkariCoreInputOutputData(),
                    channelCount = AkariCoreAudioProperties.CHANNEL_COUNT,
                    inSamplingRate = samplingRate,
                    outSamplingRate = AkariCoreAudioProperties.SAMPLING_RATE
                )
                reSamplingFile
            } else {
                // 不要
                decodeFile
            }

            // ファイル名直して終了
            fixSamplingRateDecodeFile.renameTo(outputDecodePcmFile)
        } catch (e: CancellationException) {
            // キャンセル時
            // delete で削除してしまう
            withContext(NonCancellable) {
                decodeFile.delete()
                reSamplingFile.delete()
            }
        }
    }

    /** 一時的なファイルを作る */
    private fun createTempFile(fileName: String): File = tempFolder
        .resolve("${fileName}_${Random.nextInt()}") // TODO もっといい重複しない方法
        .apply { createNewFile() }

    /**
     * デコードする、した音声のアイテム
     *
     * @param filePath 元データのパス
     * @param outputDecodePcmFile デコードした音声 PCM の保存先
     * @param decodeJob 非同期で処理されているデコード処理の[Job]。キャンセルとか待機とかで使う。
     */
    private data class AudioDecodeItem(
        val filePath: RenderData.FilePath,
        val outputDecodePcmFile: File,
        val decodeJob: Job
    )

    companion object {
        private const val AUDIO_DECODE_FILE = "audio_decode_file"
        private const val AUDIO_FIX_SAMPLING = "audio_fix_sampling"
    }
}