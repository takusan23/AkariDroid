package io.github.takusan23.akaricore.v1.data

import java.io.File

/**
 * File でも Uri でも対応できるようにしたもの
 */
interface VideoFileInterface {

    /** 動画の元データ */
    val videoFile: File

    /** 音声素材。つまりBGM */
    val audioAssetFileList: List<File>

    /** 一時保存先 */
    val tempWorkFolder: File

    /** エンコードした音声の保存先 */
    val encodedAudioFile: File
        get() = File(tempWorkFolder, TEMP_AUDIO_FILE).apply { createNewFile() }

    /** エンコードした動画の保存先 */
    val encodedVideoFile: File
        get() = File(tempWorkFolder, TEMP_VIDEO_FILE).apply { createNewFile() }

    /** コンテナフォーマット [android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4] など  */
    val containerFormat: Int

    /** エンコードされた動画の置き場 */
    val outputFile: File

    /**
     * ファイルが一時的に別途必要な場合に[File]を作ってくれる関数
     * [destroy]を呼び出すと削除される
     *
     * @param fileName ファイル名
     * @return [File]
     */
    suspend fun createCustomFile(fileName: String = "temp_${System.currentTimeMillis()}"): File {
        return File(tempWorkFolder, fileName).apply { createNewFile() }
    }

    /** エンコーダー開始前に呼ばれる */
    suspend fun prepare() {}

    /** 終了時に呼ばれる */
    suspend fun destroy() {}

    companion object {
        /** 一時映像ファイル保存先 */
        const val TEMP_VIDEO_FILE = "temp_video_file.mp4"

        /** 一時音声ファイル保存先 */
        const val TEMP_AUDIO_FILE = "temp_audio_file.aac"
    }
}