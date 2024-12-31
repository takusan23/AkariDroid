package io.github.takusan23.akaridroid.encoder

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import io.github.takusan23.akaricore.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.common.MediaMuxerTool
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoEncoder
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.audiorender.AudioRender
import io.github.takusan23.akaridroid.canvasrender.VideoTrackRenderer
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import io.github.takusan23.akaridroid.tool.ProjectFolderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [VideoEditorPreviewPlayer]のエンコード版 */
object AkariCoreEncoder {

    private const val VIDEO_TRACK_FILE_NAME = "video_track"
    private const val AUDIO_TRACK_FILE_NAME = "audio_track"
    private const val RESULT_FILE_NAME_PREFIX = "AkariDroid_Result_"

    /** プレビューで作った PCM は使わない。プレビュー準備中の可能性があるため */
    private const val ENCODE_OUT_PCM_FILE_NAME = "encode_outpcm_file"

    /** プレビューで使ってる tempFolder は消えるかもしれないので新設 */
    private const val TEMP_FOLDER_NAME = "encode_temp_folder"

    /** エンコードの進捗 */
    sealed interface EncodeStatus {

        /** エンコード中のプロジェクト名 */
        val projectName: String

        /**
         * エンコード中
         *
         * @param encodePositionMs 映像トラックのエンコード済み位置
         * @param durationMs 動画の時間
         */
        data class Progress(
            override val projectName: String,
            val encodePositionMs: Long,
            val durationMs: Long
        ) : EncodeStatus

        /** 映像トラックと音声トラックを一つのコンテナにミックス中 */
        data class Mixing(
            override val projectName: String
        ) : EncodeStatus

        /** 端末の動画フォルダへ移動中 */
        data class MoveFile(
            override val projectName: String
        ) : EncodeStatus
    }

    /**
     * エンコードする
     *
     * @param context [Context]
     * @param projectName プロジェクト名
     * @param resultFileName ファイル名
     * @param encoderParameters エンコーダーのパラメーター
     * @param onUpdateStatus エンコードの進捗。[EncodeStatus]
     * @param renderData 描画する内容。[RenderData]
     */
    suspend fun encode(
        context: Context,
        projectName: String,
        renderData: RenderData,
        encoderParameters: EncoderParameters,
        resultFileName: String = "$RESULT_FILE_NAME_PREFIX${System.currentTimeMillis()}.${encoderParameters.containerFormat.extension}",
        onUpdateStatus: (EncodeStatus) -> Unit
    ) {
        // 映像トラック生成器
        val videoRenderer = VideoTrackRenderer(context)

        // 音声トラック生成器
        // outputDecodePcmFolder は使い回せる。ファイルのハッシュを使っているので。
        val projectFolder = ProjectFolderManager.getProjectFolder(context, projectName)
        val outPcmFile = projectFolder.resolve(ENCODE_OUT_PCM_FILE_NAME)
        val audioRender = AudioRender(
            context = context,
            outPcmFile = outPcmFile,
            outputDecodePcmFolder = projectFolder.resolve(VideoEditorPreviewPlayer.DECODE_PCM_FOLDER_NAME).apply { mkdir() },
            tempFolder = projectFolder.resolve(TEMP_FOLDER_NAME).apply { mkdir() }
        )

        val durationMs = renderData.durationMs
        val videoTrackFile = projectFolder.resolve(VIDEO_TRACK_FILE_NAME)
        val audioTrackFile = projectFolder.resolve(AUDIO_TRACK_FILE_NAME)
        val resultVideoFile = projectFolder.resolve(resultFileName)

        try {
            // 映像トラック、音声トラックを作り始める
            // 最後に2つのトラックを1つの mp4 にする
            coroutineScope {
                launch(Dispatchers.Default) {
                    // 映像もエンコードする時
                    val videoParams = when (encoderParameters) {
                        is EncoderParameters.AudioOnly -> return@launch
                        is EncoderParameters.AudioVideo -> encoderParameters.videoEncoderParameters
                    }

                    // エンコーダー
                    val akariVideoEncoder = AkariVideoEncoder().apply {
                        prepare(
                            output = videoTrackFile.toAkariCoreInputOutputData(),
                            codecName = videoParams.codec.androidMediaCodecName,
                            bitRate = videoParams.bitrate,
                            frameRate = videoParams.frameRate,
                            keyframeInterval = videoParams.keyframeInterval,
                            outputVideoWidth = renderData.videoSize.width,
                            outputVideoHeight = renderData.videoSize.height,
                            containerFormat = encoderParameters.containerFormat.androidMediaMuxerFormat,
                            tenBitHdrParametersOrNullSdr = if (renderData.isEnableTenBitHdr && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                AkariVideoEncoder.TenBitHdrParameters(
                                    codecProfile = MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
                                    colorStandard = MediaFormat.COLOR_STANDARD_BT2020,
                                    colorTransfer = MediaFormat.COLOR_TRANSFER_HLG
                                )
                            } else null
                        )
                    }

                    // 出力先と動画素材を渡す
                    videoRenderer.setOutputSurface(
                        surface = akariVideoEncoder.getInputSurface()
                    )
                    videoRenderer.setVideoParameters(
                        outputWidth = renderData.videoSize.width,
                        outputHeight = renderData.videoSize.height,
                        isEnableTenBitHdr = renderData.isEnableTenBitHdr
                    )
                    videoRenderer.setRenderData(
                        canvasRenderItem = renderData.canvasRenderItem
                    )

                    // エンコーダー開始
                    val encoderJob = launch { akariVideoEncoder.start() }

                    // 描画も開始
                    val graphicsJob = launch {
                        try {
                            videoRenderer.drawRecordLoop(
                                durationMs = durationMs,
                                frameRate = videoParams.frameRate,
                                onProgress = { currentPositionMs ->
                                    onUpdateStatus(
                                        EncodeStatus.Progress(
                                            projectName = projectName,
                                            encodePositionMs = currentPositionMs,
                                            durationMs = durationMs
                                        )
                                    )
                                }
                            )
                        } finally {
                            videoRenderer.destroy()
                        }
                    }

                    // 描画が終わるまで待ってその後エンコーダーも止める
                    graphicsJob.join()
                    encoderJob.cancelAndJoin()
                }
                launch(Dispatchers.Default) {
                    val audioParams = when (encoderParameters) {
                        is EncoderParameters.AudioOnly -> encoderParameters.audioEncoderParameters
                        is EncoderParameters.AudioVideo -> encoderParameters.audioEncoderParameters
                    }
                    // 音声素材をデコードして、合成済みの音声を作成する
                    audioRender.setRenderData(
                        audioRenderItem = renderData.audioRenderItem,
                        durationMs = durationMs
                    )
                    // エンコードする
                    AudioEncodeDecodeProcessor.encode(
                        input = outPcmFile.toAkariCoreInputOutputData(),
                        output = audioTrackFile.toAkariCoreInputOutputData(),
                        codecName = audioParams.codec.androidMediaCodecName,
                        bitRate = audioParams.bitrate,
                        containerFormat = encoderParameters.containerFormat.androidMediaMuxerFormat,
                    )
                }
            }

            // 映像トラックと音声トラックを一緒にする
            onUpdateStatus(EncodeStatus.Mixing(projectName = projectName))
            MediaMuxerTool.mixed(
                output = resultVideoFile.toAkariCoreInputOutputData(),
                containerFormatTrackInputList = listOf(
                    videoTrackFile.toAkariCoreInputOutputData(),
                    audioTrackFile.toAkariCoreInputOutputData()
                ),
                containerFormat = encoderParameters.containerFormat.androidMediaMuxerFormat,
            )

            // 動画フォルダへコピーする
            onUpdateStatus(EncodeStatus.MoveFile(projectName = projectName))
            MediaStoreTool.copyToVideoFolder(
                context = context,
                file = resultVideoFile
            )
        } finally {
            // 消す
            videoTrackFile.delete()
            audioTrackFile.delete()
            resultVideoFile.delete()
            outPcmFile.delete()
            // リソース開放
            videoRenderer.destroy()
            audioRender.destroy()
        }
    }
}