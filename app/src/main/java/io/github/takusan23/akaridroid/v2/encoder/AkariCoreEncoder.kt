package io.github.takusan23.akaridroid.v2.encoder

import android.content.Context
import io.github.takusan23.akaricore.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.common.MediaMuxerTool
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.video.CanvasVideoProcessor
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import io.github.takusan23.akaridroid.v2.RenderData
import io.github.takusan23.akaridroid.v2.audiorender.AudioRender
import io.github.takusan23.akaridroid.v2.canvasrender.CanvasRender
import io.github.takusan23.akaridroid.v2.preview.VideoEditorPreviewPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** [VideoEditorPreviewPlayer]のエンコード版 */
object AkariCoreEncoder {

    private const val VIDEO_TRACK_FILE_NAME = "video_track"
    private const val AUDIO_TRACK_FILE_NAME = "audio_track"
    private const val RESULT_FILE_NAME_PREFIX = "AkariDroid_Result_"

    /**
     * エンコードする
     *
     * @param context [Context]
     * @param projectFolder PCM とかの保存先
     * @param renderData 描画する内容 [RenderData]
     */
    suspend fun encode(
        context: Context,
        projectFolder: File,
        renderData: RenderData
    ): Unit = withContext(Dispatchers.IO) {
        // 映像トラック生成器
        val canvasRender = CanvasRender(
            context = context
        )
        // 音声トラック生成器
        val audioRender = AudioRender(
            context = context,
            outPcmFile = projectFolder.resolve(VideoEditorPreviewPlayer.OUT_PCM_FILE_NAME),
            outputDecodePcmFolder = projectFolder.resolve(VideoEditorPreviewPlayer.DECODE_PCM_FOLDER_NAME).apply { mkdir() },
            tempFolder = projectFolder.resolve(VideoEditorPreviewPlayer.TEMP_FOLDER_NAME).apply { mkdir() }
        )

        val durationMs = renderData.durationMs
        val videoTrackFile = projectFolder.resolve(VIDEO_TRACK_FILE_NAME)
        val audioTrackFile = projectFolder.resolve(AUDIO_TRACK_FILE_NAME)
        val resultVideoFile = projectFolder.resolve("$RESULT_FILE_NAME_PREFIX${System.currentTimeMillis()}.mp4")

        try {
            // 映像トラック、音声トラックを作り始める
            // 最後に2つのトラックを1つの mp4 にする
            listOf(
                launch {
                    // 素材を入れる
                    canvasRender.setRenderData(
                        canvasRenderItem = renderData.canvasRenderItem
                    )
                    // 動画のフレームを作る
                    CanvasVideoProcessor.start(
                        output = videoTrackFile.toAkariCoreInputOutputData(),
                        onCanvasDrawRequest = { positionMs ->
                            canvasRender.draw(
                                canvas = this,
                                durationMs = durationMs,
                                currentPositionMs = positionMs
                            )
                            positionMs <= durationMs
                        }
                    )
                },
                launch {
                    // 音声素材をデコードして、合成済みの音声を作成する
                    audioRender.setRenderData(
                        audioRenderItem = renderData.audioRenderItem,
                        durationMs = durationMs
                    )
                    // エンコードする
                    AudioEncodeDecodeProcessor.encode(
                        input = audioRender.outPcmFile.toAkariCoreInputOutputData(),
                        output = audioTrackFile.toAkariCoreInputOutputData()
                    )
                }
            ).joinAll() // 両方終わるのを待つ

            // 映像トラックと音声トラックを一緒にする
            MediaMuxerTool.mixed(
                output = resultVideoFile.toAkariCoreInputOutputData(),
                containerFormatTrackInputList = listOf(
                    videoTrackFile.toAkariCoreInputOutputData(),
                    audioTrackFile.toAkariCoreInputOutputData()
                )
            )

            // 動画フォルダへコピーする
            MediaStoreTool.copyToVideoFolder(
                context = context,
                file = resultVideoFile
            )
        } finally {
            // 消す
            videoTrackFile.delete()
            audioTrackFile.delete()
            resultVideoFile.delete()
            // リソース開放
            canvasRender.destroy()
            audioRender.destroy()
        }
    }
}