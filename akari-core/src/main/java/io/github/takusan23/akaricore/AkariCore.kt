package io.github.takusan23.akaricore

import android.graphics.Canvas
import android.media.MediaMuxer
import io.github.takusan23.akaricore.data.AudioEncoderData
import io.github.takusan23.akaricore.data.VideoEncoderData
import io.github.takusan23.akaricore.data.VideoFileInterface
import io.github.takusan23.akaricore.processor.AudioProcessor
import io.github.takusan23.akaricore.processor.QtFastStart
import io.github.takusan23.akaricore.processor.VideoProcessor
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
    videoEncoderData: VideoEncoderData,
    audioEncoderData: AudioEncoderData,
) {

    /** 音声エンコーダー */
    private val audioProcessor by lazy {
        AudioProcessor(
            videoFile = videoFileData.videoFile,
            resultFile = videoFileData.encodedAudioFile,
            audioCodec = audioEncoderData.codecName,
            containerFormat = videoFileData.containerFormat,
            bitRate = audioEncoderData.bitRate
        )
    }

    /** フィルターをかけられる 映像エンコーダー */
    private val videoProcessor by lazy {
        VideoProcessor(
            videoFile = videoFileData.videoFile,
            resultFile = videoFileData.encodedVideoFile,
            videoCodec = videoEncoderData.codecName,
            containerFormat = videoFileData.containerFormat,
            bitRate = videoEncoderData.bitRate,
            frameRate = videoEncoderData.frameRate,
            videoWidth = videoEncoderData.width,
            videoHeight = videoEncoderData.height
        )
    }

    /**
     * 処理を始める
     *
     * @param onCanvasDrawRequest Canvasへ描画リクエストが来た際に呼ばれる。Canvasと再生時間（ミリ秒）が渡されます
     */
    suspend fun start(
        onCanvasDrawRequest: Canvas.(positionMs: Long) -> Unit,
    ) = withContext(Dispatchers.Default) {
        videoFileData.prepare()
        val videoTask = async { videoProcessor.start(onCanvasDrawRequest) }
        val audioTask = async { audioProcessor.start() }
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

    suspend fun stop() {
        audioProcessor.stop()
        videoProcessor.stop()
        videoFileData.destroy()
    }

}