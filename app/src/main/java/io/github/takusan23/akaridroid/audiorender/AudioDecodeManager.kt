package io.github.takusan23.akaridroid.audiorender

import android.content.Context
import android.media.MediaFormat
import androidx.core.net.toUri
import io.github.takusan23.akaricore.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.audio.AudioMonoToStereoProcessor
import io.github.takusan23.akaricore.audio.AudioSonicProcessor
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.tool.FileHashTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [AudioRender]から PCM デコード関連のコードがここにある。
 * デコード開始、終了、デコード済み一覧等。
 * 複数の音声ファイルをそれぞれ PCM にデコードするやつ。
 * できる限り音声ファイルのデコードを避けるため、ファイルのハッシュ値を PDM のファイル名にして、デコード前にデコード済みか判断します。
 *
 * [RenderData.AudioItem]ではなく、[RenderData.FilePath]をキーにしているのは同じファイルの[RenderData.AudioItem]ならスキップさせるため
 *
 * @param context [Context]
 * @param tempFolder 一時的な保存先
 * @param outputDecodePcmFolder PCM デコード結果を保存するフォルダ
 */
class AudioDecodeManager(
    private val context: Context,
    private val tempFolder: File,
    private val outputDecodePcmFolder: File
) {

    /** PCM デコード用コルーチンスコープ */
    private val decodeScope = CoroutineScope(Dispatchers.Default + Job())

    /** デコーダーが枯渇しないように、並列実行数を制限するセマフォ */
    private val semaphore = Semaphore(permits = MAX_DECODE_CONCURRENCY)

    /** デコード中の[AudioDecodeItem]配列。キャンセルで使う */
    private var progressDecodeItemList = listOf<AudioDecodeItem>()

    /** [createTempFile]で重複しないように */
    private var uniqueCount = 0

    /**
     * 既にデコードが完了しているか、[addDecode]でデコードが進行中の場合は true
     *
     * @param filePath Uri か File
     */
    suspend fun hasDecodedOrProgressDecoding(filePath: RenderData.FilePath): Boolean {
        return when {
            createPcmFile(filePath).exists() -> true // 既にあれば
            getProgressDecodeItemOrNull(filePath)?.decodeJob?.isActive == true -> true // 進行中であれば
            else -> false
        }
    }

    /**
     * デコードするファイルを追加する。
     * コルーチンをキャンセルしても、デコード処理は別のコルーチンスコープなのでキャンセルされません。
     *
     * @param filePath 音声ファイル
     */
    fun addDecode(filePath: RenderData.FilePath) {
        // 非同期でデコードさせる
        val decodeJob = decodeScope.launch {
            // 並列数を制限する
            semaphore.withPermit {
                try {
                    // デコード
                    decode(filePath)
                } finally {
                    // デコード中から削除する
                    val currentItem = getProgressDecodeItemOrNull(filePath)
                    if (currentItem != null) {
                        progressDecodeItemList = progressDecodeItemList - currentItem
                    }
                }
            }
        }
        // 追加する
        progressDecodeItemList = progressDecodeItemList + AudioDecodeItem(filePath, decodeJob)
    }

    /**
     * デコード済みならファイルを消す。
     * デコード中ならキャンセルして、ファイルも消す。
     *
     * @param filePath 音声ファイル
     */
    suspend fun cancelDecodeAndDeleteFile(filePath: RenderData.FilePath) {
        // デコードしたファイルを消す
        createPcmFile(filePath).delete()
        // デコード中ならキャンセル
        val exitsItem = getProgressDecodeItemOrNull(filePath) ?: return
        // キャンセルしてデコードしたファイルも消す
        exitsItem.decodeJob.cancelAndJoin()
        progressDecodeItemList = progressDecodeItemList - exitsItem
    }

    /**
     * すべてのデコードを待つ
     * [addDecode]を呼び出した後にこれで待つ
     */
    suspend fun awaitAllDecode() {
        progressDecodeItemList.map { it.decodeJob }.joinAll()
    }

    /**
     * デコード済みのファイルを取得する。デコード済みならすぐ返す。
     *
     * @param filePath 音声ファイル
     * @return デコード済みならファイル、ない場合、デコード終わってない場合は null
     */
    suspend fun getDecodedPcmFile(filePath: RenderData.FilePath): File? {
        // デコードが完了するまで createNewFile しないため
        return createPcmFile(filePath).takeIf { it.exists() }
    }

    /**
     * 破棄する。
     * [outputDecodePcmFolder]は別途消してください（プレビューと動画書き出しで使い回す？）。
     */
    fun destroy() {
        decodeScope.cancel()
    }

    /**
     * デコード結果を保存する[File]を返す。ファイル名がハッシュになります。
     * [File.createNewFile]はデコードが終わるまで作らないでください。[File.exists]でデコード済み判定をするので。
     *
     * @param filePath Uri か File
     * @return ファイル
     */
    private suspend fun createPcmFile(filePath: RenderData.FilePath): File = withContext(Dispatchers.IO) {
        val fileHash = FileHashTool.calcMd5(context, filePath)
        outputDecodePcmFolder.resolve("$PREFIX_DECODE_PCM_FILE$fileHash")
    }

    /**
     * [filePath]からデコード中の[AudioDecodeItem]を探す
     *
     * @param filePath デコード中ファイルの[RenderData.FilePath]
     * @return デコード中の[AudioDecodeItem]
     */
    private fun getProgressDecodeItemOrNull(filePath: RenderData.FilePath): AudioDecodeItem? = progressDecodeItemList.firstOrNull { it.filePath == filePath }

    /**
     * デコードする
     *
     * @param filePath ファイルパス
     */
    private suspend fun decode(filePath: RenderData.FilePath) {
        val decodeFile = createTempFile(AUDIO_DECODE_FILE)
        val reSamplingFile = createTempFile(AUDIO_FIX_SAMPLING)
        val monoToStereoFile = createTempFile(AUDIO_FIX_MONO_TO_STEREO)

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

            val samplingRate = decoderMediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = decoderMediaFormat!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // 追加で修正が必要な場合
            val fixFile = decodeFile
                // サンプリングレート変換が必要な場合
                .let { inputFile ->
                    if (samplingRate != AkariCoreAudioProperties.SAMPLING_RATE) {
                        reSamplingFile.also { reSamplingFile ->
                            AudioSonicProcessor.reSamplingBySonic(
                                input = inputFile.toAkariCoreInputOutputData(),
                                output = reSamplingFile.toAkariCoreInputOutputData(),
                                channelCount = channelCount,
                                inSamplingRate = samplingRate,
                                outSamplingRate = AkariCoreAudioProperties.SAMPLING_RATE
                            )
                        }
                    } else {
                        // 不要
                        inputFile
                    }
                }
                // ステレオ音声じゃない場合（チャンネル数が 2 じゃない場合）
                .let { inputFile ->
                    if (channelCount != AkariCoreAudioProperties.CHANNEL_COUNT) {
                        monoToStereoFile.also { monoToStereoFile ->
                            AudioMonoToStereoProcessor.start(
                                input = inputFile.toAkariCoreInputOutputData(),
                                output = monoToStereoFile.toAkariCoreInputOutputData()
                            )
                        }
                    } else {
                        // 不要
                        inputFile
                    }
                }

            // ファイル名直して終了
            val outputDecodeFile = createPcmFile(filePath).apply { createNewFile() }
            fixFile.renameTo(outputDecodeFile)
        } finally {
            // デコードが終わったら消す
            withContext(NonCancellable) {
                decodeFile.delete()
                reSamplingFile.delete()
                monoToStereoFile.delete()
            }
        }
    }

    /** 一時的なファイルを作る */
    private fun createTempFile(fileName: String): File = tempFolder
        .resolve("${fileName}_${uniqueCount++}")
        .apply { createNewFile() }

    /**
     * デコード中のアイテム
     *
     * @param filePath 元データのパス
     * @param decodeJob 非同期で処理されているデコード処理の[Job]。キャンセルとか待機とかで使う。
     */
    private data class AudioDecodeItem(
        val filePath: RenderData.FilePath,
        val decodeJob: Job
    )

    companion object {
        private const val AUDIO_DECODE_FILE = "audio_decode_file"
        private const val AUDIO_FIX_SAMPLING = "audio_fix_sampling"
        private const val AUDIO_FIX_MONO_TO_STEREO = "audio_fix_mono_to_stereo"
        private const val PREFIX_DECODE_PCM_FILE = "pcm_file_"

        /** 音声デコードの並列上限。大きすぎるとデコーダーが枯渇してクラッシュする。少なすぎるとなかなか終わらない。 */
        private const val MAX_DECODE_CONCURRENCY = 4 // TODO 適当すぎる、ちゃんと調べる
    }
}