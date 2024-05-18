package io.github.takusan23.akaridroid.audiorender

import io.github.takusan23.akaricore.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.audio.AudioSonicProcessor
import io.github.takusan23.akaricore.audio.AudioVolumeProcessor
import io.github.takusan23.akaricore.common.AkariCoreInputOutput
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

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

    /** デコード済みファイルを読み出すための[InputStream]。read は巻き戻せないので、戻る場合は作り直して。 */
    private var inputStream: FileInputStream? = null

    /**
     * 速度調整のために、1秒ごとに PCM 音声データを分割してデータを処理する
     * ただ、[readPcmData]はミリ秒単位で呼び出されるため、必要になったら更新されるように。
     */
    private var applyEffectByteArrayStream = byteArrayOf().inputStream() // 最初は空で

    override suspend fun prepareRead(): Unit = withContext(Dispatchers.IO) {
        // InputStream を開く
        inputStream?.close()
        inputStream = decodePcmFile.inputStream()
        // 音声ファイルをカットする場合
        // 読み出し開始位置を skip して調整しておく
        val skipBytes = AkariCoreAudioProperties.ONE_MILLI_SECONDS_PCM_DATA_SIZE * audioItem.displayOffset.offsetFirstMs
        inputStream?.skip(skipBytes)
    }

    override suspend fun readPcmData(readSize: Int): ByteArray = withContext(Dispatchers.IO) {
        // 秒単位で処理されたデータから、ミリ秒単位の要求には秒単位で計算済みのバイト配列を返す
        val readByteArray = ByteArray(readSize)
        val size = applyEffectByteArrayStream.read(readByteArray)

        // なかった場合は次の1秒間のデータを
        if (size == -1) {
            applyEffectByteArrayStream.close()
            // 次の1秒間のデータを取り出し、再生速度とかを適用する
            val oneSecondsByteArray = ByteArray(AkariCoreAudioProperties.ONE_SECOND_PCM_DATA_SIZE)
            inputStream?.read(oneSecondsByteArray)
            applyEffectByteArrayStream = applyEffectPcmData(oneSecondsByteArray).inputStream()
            // ByteArray 読み出して返り値に
            applyEffectByteArrayStream.read(readByteArray)
        }

        readByteArray
    }

    /**
     * 音量や速度調整を適用したバイト配列を生成する
     *
     * @param byteArray 読み出した PCM データ。最低でも1秒間くらいは無いと再生速度が速くなりすぎる？
     * @return 適用された[ByteArray]
     */
    private suspend fun applyEffectPcmData(byteArray: ByteArray): ByteArray {
        return byteArray
            // 音量調整を適用する
            .let { input ->
                if (audioItem.volume != RenderData.AudioItem.DEFAULT_VOLUME) {
                    val output = AkariCoreInputOutput.OutputJavaByteArray()
                    AudioVolumeProcessor.start(
                        input = input.toAkariCoreInputOutputData(),
                        output = output,
                        volume = audioItem.volume
                    )
                    output.byteArray
                } else {
                    input
                }
            }
            // 再生速度を適用する
            .let { input ->
                if (audioItem.displayTime.playbackSpeed != RenderData.DisplayTime.DEFAULT_PLAYBACK_SPEED) {
                    val output = AkariCoreInputOutput.OutputJavaByteArray()
                    AudioSonicProcessor.playbackSpeedBySonic(
                        input = input.toAkariCoreInputOutputData(),
                        output = output,
                        samplingRate = AkariCoreAudioProperties.SAMPLING_RATE,
                        channelCount = AkariCoreAudioProperties.CHANNEL_COUNT,
                        speed = audioItem.displayTime.playbackSpeed
                    )
                    output.byteArray
                } else {
                    input
                }
            }
    }

    override fun isDisplayPosition(currentPositionMs: Long): Boolean = currentPositionMs in audioItem.displayTime

    /** 破棄する */
    override fun destroy() {
        inputStream?.close()
    }
}