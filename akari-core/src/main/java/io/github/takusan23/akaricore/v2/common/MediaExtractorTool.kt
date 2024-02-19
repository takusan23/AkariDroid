package io.github.takusan23.akaricore.v2.common

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [MediaExtractor]を簡単に扱えるようにしたもの
 * */
object MediaExtractorTool {

    /**
     * 引数に渡した動画[AkariCoreInputOutput.Input]の情報を[MediaExtractor]で取り出す
     *
     * @param input 動画ファイル。詳しくは[AkariCoreInputOutput]
     * @param mimeType [ExtractMimeType]。音声、動画どっちか
     * @return [MediaExtractor] / [mimeType]のトラック番号 / [MediaFormat]
     */
    suspend fun extractMedia(
        input: AkariCoreInputOutput.Input,
        mimeType: ExtractMimeType
    ): Triple<MediaExtractor, Int, MediaFormat>? = withContext(Dispatchers.IO) {
        val mediaExtractor = MediaExtractor()
        when (input) {
            is AkariCoreInputOutput.AndroidUri -> input.getFileDescriptor().use { mediaExtractor.setDataSource(it.fileDescriptor) }
            is AkariCoreInputOutput.JavaFile -> mediaExtractor.setDataSource(input.filePath)
            is AkariCoreInputOutput.InputJavaByteArray -> {
                // TODO Android 6 以降のみ！
                if (input.mediaDataSource != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mediaExtractor.setDataSource(input.mediaDataSource)
                    }
                }
            }
        }
        // 映像トラックとインデックス番号のPairを作って返す
        val (index, track) = (0 until mediaExtractor.trackCount)
            .map { index -> index to mediaExtractor.getTrackFormat(index) }
            .firstOrNull { (_, track) -> track.getString(MediaFormat.KEY_MIME)?.startsWith(mimeType.startWidth) == true } ?: return@withContext null
        return@withContext Triple(mediaExtractor, index, track)
    }

    /** [extractMedia]で渡すデータ */
    enum class ExtractMimeType(val startWidth: String) {
        EXTRACT_MIME_AUDIO("audio/"),
        EXTRACT_MIME_VIDEO("video/")
    }

}