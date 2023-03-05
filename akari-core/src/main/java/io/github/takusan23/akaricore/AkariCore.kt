package io.github.takusan23.akaricore

import android.graphics.Canvas
import android.media.MediaMuxer
import io.github.takusan23.akaricore.data.AudioEncoderData
import io.github.takusan23.akaricore.data.VideoEncoderData
import io.github.takusan23.akaricore.data.VideoFileInterface
import io.github.takusan23.akaricore.processor.AudioMixingProcessor
import io.github.takusan23.akaricore.processor.QtFastStart
import io.github.takusan23.akaricore.processor.VideoCanvasProcessor
import io.github.takusan23.akaricore.tool.MediaMuxerTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * OpenGLで映像の上にCanvasを重ねて合成する
 *
 * @param videoFileData 動画ファイルについて
 * @param videoEncoderData 映像エンコーダーについて
 * @param audioEncoderData 音声エンコーダーについて
 */
class AkariCore(
    private val videoFileData: VideoFileInterface,
    private val videoEncoderData: VideoEncoderData,
    private val audioEncoderData: AudioEncoderData,
) {
    /**
     * 処理を始める
     *
     * @param onCanvasDrawRequest Canvasへ描画リクエストが来た際に呼ばれる。Canvasと再生時間（ミリ秒）が渡されます
     */
    suspend fun start(
        onCanvasDrawRequest: Canvas.(positionMs: Long) -> Unit,
    ) = withContext(Dispatchers.Default) {
        videoFileData.prepare()
        val videoTask = async {
            VideoCanvasProcessor.start(
                videoFile = videoFileData.videoFile,
                resultFile = videoFileData.encodedVideoFile,
                videoCodec = videoEncoderData.codecName,
                containerFormat = videoFileData.containerFormat,
                bitRate = videoEncoderData.bitRate,
                frameRate = videoEncoderData.frameRate,
                outputVideoWidth = videoEncoderData.width,
                outputVideoHeight = videoEncoderData.height,
                onCanvasDrawRequest = onCanvasDrawRequest
            )
        }
        val audioTask = async {
            AudioMixingProcessor.start(
                audioFileList = listOf(videoFileData.videoFile) + videoFileData.audioAssetFileList,
                resultFile = videoFileData.encodedAudioFile,
                tempFolder = videoFileData.tempWorkFolder,
                audioCodec = audioEncoderData.codecName,
                bitRate = audioEncoderData.bitRate,
                mixingVolume = audioEncoderData.mixingVolume
            )
        }
        // 終わるまで待つ
        videoTask.await()
        audioTask.await()
        // 音声と映像をコンテナフォーマットへ
        // コンテナフォーマットがmp4の場合は別途処理をする
        if (videoFileData.containerFormat == MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4) {
            // とりあえずMixする
            val tempMixedFile = videoFileData.createCustomFile()
            MediaMuxerTool.mixed(
                resultFile = tempMixedFile,
                containerFormat = videoFileData.containerFormat,
                mergeFileList = listOf(videoFileData.encodedAudioFile, videoFileData.encodedVideoFile)
            )
            // ストリーミング可能な形式のmp4へ変換する
            // mp4 ファイルのバイナリの中から、 moovブロック を見つけて、そのブロックを先頭に持ってきます
            // MediaMuxerが作る mp4 は moovブロック が最後に配置されており、再生する際にすべてダウンロードする必要があります
            // 詳しくは faststart とかで調べてください
            QtFastStart.fastStart(tempMixedFile, videoFileData.outputFile)
        } else {
            MediaMuxerTool.mixed(
                resultFile = videoFileData.outputFile,
                containerFormat = videoFileData.containerFormat,
                mergeFileList = listOf(videoFileData.encodedAudioFile, videoFileData.encodedVideoFile)
            )
        }
        // 一時ファイルを消して完成
        videoFileData.destroy()
    }

}