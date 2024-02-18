package io.github.takusan23.akaricore.v2.common

import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [MediaExtractor]を簡単に扱えるようにしたもの
 * */
object MediaExtractorTool {

    /**
     * 引数に渡した動画パス[videoPath]の情報を[MediaExtractor]で取り出す
     *
     * @param inputDataSource Uri なら[AkariCoreInputDataSource.AndroidUri]、File なら[AkariCoreInputDataSource.JavaFile]
     * @param mimeType [ExtractMimeType]。音声、動画どっちか
     * @return [MediaExtractor] / [mimeType]のトラック番号 / [MediaFormat]
     */
    suspend fun extractMedia(
        inputDataSource: AkariCoreInputDataSource,
        mimeType: ExtractMimeType
    ): Triple<MediaExtractor, Int, MediaFormat>? = withContext(Dispatchers.IO) {
        val mediaExtractor = MediaExtractor()
        when (inputDataSource) {
            is AkariCoreInputDataSource.AndroidUri -> inputDataSource.getFileDescriptor().use { mediaExtractor.setDataSource(it.fileDescriptor) }
            is AkariCoreInputDataSource.JavaFile -> mediaExtractor.setDataSource(inputDataSource.file.path)
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