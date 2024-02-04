package io.github.takusan23.akaridroid.v2.audiorender

import android.media.MediaFormat
import io.github.takusan23.akaricore.v2.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.v2.audio.AkariCoreAudioProperties
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
        // カットが必要な場合
        val audioFile = if (audioItem.cropTimeCrop != null) {
            tempFolder.resolve(AUDIO_CROP_FILE).also { cropAudioFile ->
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
        val decodeFile = tempFolder.resolve(AUDIO_DECODE_FILE)
        AudioEncodeDecodeProcessor.decode(
            inAudioFile = audioFile,
            outPcmFile = decodeFile,
            onOutputFormat = { decoderMediaFormat = it }
        )

        val samplingRate = decoderMediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        if (samplingRate == AkariCoreAudioProperties.SAMPLING_RATE) {
            // 不要なら入れ直して終了
            decodeFile.renameTo(outPcmFile)
        } else {
            // サンプリングレート変換が必要な場合
            // TODO チャンネル数は？
            ReSamplingRateProcessor.reSamplingBySonic(
                inPcmFile = decodeFile,
                outPcmFile = outPcmFile,
                channelCount = 2,
                inSamplingRate = samplingRate,
                outSamplingRate = AkariCoreAudioProperties.SAMPLING_RATE
            )
        }
    }

    override suspend fun isEquals(item: RenderData.AudioItem): Boolean {
        return audioItem != item
    }

    companion object {

        private const val AUDIO_CROP_FILE = "audio_crop_file"
        private const val AUDIO_DECODE_FILE = "audio_decode_file"

    }
}