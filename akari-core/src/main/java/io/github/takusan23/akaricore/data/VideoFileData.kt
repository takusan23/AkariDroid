package io.github.takusan23.akaricore.data

import java.io.File

/**
 * 動画ファイル関連、コンテナフォーマットとか
 *
 * @param videoFile 動画ファイル
 * @param tempWorkFolder 一時的にファイルを置くためそのためのフォルダ、終了後に削除されます
 * @param containerFormat フォーマット [android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4] など
 * @param outputFile 完成品置き場
 */
class VideoFileData(
    override val videoFile: File,
    override val tempWorkFolder: File,
    override val containerFormat: Int,
    override val outputFile: File,
) : VideoFileInterface {
    override suspend fun destroy() {
        tempWorkFolder.deleteRecursively()
    }
}