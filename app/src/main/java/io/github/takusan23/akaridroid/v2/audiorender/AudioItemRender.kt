package io.github.takusan23.akaridroid.v2.audiorender

import android.media.MediaFormat
import io.github.takusan23.akaricore.v2.audio.AkariCoreAudioProperties
import io.github.takusan23.akaricore.v2.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.v2.audio.AudioVolumeProcessor
import io.github.takusan23.akaricore.v2.audio.ReSamplingRateProcessor
import io.github.takusan23.akaricore.v2.common.CutProcessor
import io.github.takusan23.akaricore.v2.common.MediaExtractorTool
import io.github.takusan23.akaridroid.v2.canvasrender.RenderData
import java.io.File

/**
 * [io.github.takusan23.akaridroid.v2.canvasrender.RenderData.AudioItem]からデータをデコードしたりするクラス
 *
 * @param audioItem 音声素材
 * @param outPcmFile デコードしたファイルの保存先
 */
class AudioItemRender(
    private val audioItem: RenderData.AudioItem.Audio,
    override val outPcmFile: File,
) : AudioRenderInterface {

    override val displayTime: RenderData.DisplayTime
        get() = audioItem.displayTime

    override suspend fun decode(tempFolder: File) {

        /** 仮のファイルを作る */
        fun createTempFile(fileName: String): File = tempFolder.resolve(fileName)

        // カットが必要な場合
        val audioFile = if (audioItem.cropTimeCrop != null) {
            createTempFile(AUDIO_CROP_FILE).also { cropAudioFile ->
                CutProcessor.start(
                    targetVideoFile = File(audioItem.filePath),
                    resultFile = cropAudioFile,
                    timeRangeMs = audioItem.cropTimeCrop,
                    extractMimeType = MediaExtractorTool.ExtractMimeType.EXTRACT_MIME_AUDIO
                )
            }
        } else {
            File(audioItem.filePath)
        }

        // デコーダーにかける
        var decoderMediaFormat: MediaFormat? = null
        val decodeFile = createTempFile(AUDIO_DECODE_FILE)
        AudioEncodeDecodeProcessor.decode(
            inAudioFile = audioFile,
            outPcmFile = decodeFile,
            onOutputFormat = { decoderMediaFormat = it }
        )

        // サンプリングレート変換が必要な場合
        // TODO チャンネル数は？
        val samplingRate = decoderMediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val fixSamplingRateDecodeFile = if (samplingRate != AkariCoreAudioProperties.SAMPLING_RATE) {
            createTempFile(AUDIO_FIX_SAMPLIN).also { outFile ->
                ReSamplingRateProcessor.reSamplingBySonic(
                    inPcmFile = decodeFile,
                    outPcmFile = outFile,
                    channelCount = 2,
                    inSamplingRate = samplingRate,
                    outSamplingRate = AkariCoreAudioProperties.SAMPLING_RATE
                )
            }
        } else {
            // 不要
            decodeFile
        }

        // 音量調整が必要な場合
        val fixVolumeDecodeFile = if (audioItem.volume != RenderData.AudioItem.DEFAULT_VOLUME) {
            createTempFile(AUDIO_FIX_VOLUME).also { outFile ->
                AudioVolumeProcessor.start(
                    inPcmFile = fixSamplingRateDecodeFile,
                    outPcmFile = outFile,
                    volume = audioItem.volume
                )
            }
        } else {
            // 不要
            fixSamplingRateDecodeFile
        }

        // 入れ直して終了
        fixVolumeDecodeFile.renameTo(outPcmFile)
    }

    override suspend fun isEquals(item: RenderData.AudioItem): Boolean {
        return audioItem != item
    }

    companion object {

        private const val AUDIO_CROP_FILE = "audio_crop_file"
        private const val AUDIO_DECODE_FILE = "audio_decode_file"
        private const val AUDIO_FIX_VOLUME = "audio_fix_volume"
        private const val AUDIO_FIX_SAMPLIN = "audio_fix_sampling"

    }
}