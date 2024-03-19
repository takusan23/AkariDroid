package io.github.takusan23.akaridroid.tool

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import io.github.takusan23.akaridroid.tool.data.AvAnalyzeResult
import io.github.takusan23.akaridroid.tool.data.IoType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 音声、映像、画像の解析をする
 * 意味深な名前
 */
object AvAnalyze {

    /**
     * 画像を解析する
     *
     * @param ioType Uri か File
     * @param context [Context]
     * @return [AvAnalyzeResult.Image]
     */
    suspend fun analyzeImage(
        context: Context,
        ioType: IoType
    ): AvAnalyzeResult.Image? = withContext(Dispatchers.IO) {
        when (ioType) {
            is IoType.AndroidUri -> {
                val column = arrayOf(
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT
                )
                context.contentResolver.query(ioType.uri, column, null, null, null)?.use { cursor ->
                    cursor.moveToFirst()
                    val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                    val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                    AvAnalyzeResult.Image(size = AvAnalyzeResult.Size(width, height))
                }
            }

            is IoType.JavaFile -> {
                val bitmap = BitmapFactory.decodeFile(ioType.file.path)
                AvAnalyzeResult.Image(size = AvAnalyzeResult.Size(bitmap.width, bitmap.height))
            }
        }
    }

    /**
     * 音声を解析する
     *
     * @param ioType Uri か File
     * @param context [Context]
     * @return [AvAnalyzeResult.Audio]
     */
    suspend fun analyzeAudio(
        context: Context,
        ioType: IoType
    ): AvAnalyzeResult.Audio? = withContext(Dispatchers.IO) {
        // MediaStore じゃ取れなかった
        MediaMetadataRetriever().use { mediaMetadataRetriever ->
            when (ioType) {
                is IoType.AndroidUri -> mediaMetadataRetriever.setDataSource(context, ioType.uri)
                is IoType.JavaFile -> mediaMetadataRetriever.setDataSource(ioType.file.path)
            }
            val durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: return@withContext null
            AvAnalyzeResult.Audio(durationMs = durationMs)
        }
    }

    /**
     * 動画を解析する
     *
     * @param ioType Uri か File
     * @param context [Context]
     * @return [AnalyzeResult.Video]
     */
    suspend fun analyzeVideo(
        context: Context,
        ioType: IoType
    ): AvAnalyzeResult.Video? = withContext(Dispatchers.IO) {
        // MediaStore じゃ取れなかった
        MediaMetadataRetriever().use { mediaMetadataRetriever ->
            when (ioType) {
                is IoType.AndroidUri -> mediaMetadataRetriever.setDataSource(context, ioType.uri)
                is IoType.JavaFile -> mediaMetadataRetriever.setDataSource(ioType.file.path)
            }
            val width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: return@withContext null
            val height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: return@withContext null
            val durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: return@withContext null
            val trackCount = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)?.toInt() ?: return@withContext null

            AvAnalyzeResult.Video(
                size = AvAnalyzeResult.Size(width, height),
                durationMs = durationMs,
                hasAudioTrack = trackCount == 2
            )
        }
    }

}