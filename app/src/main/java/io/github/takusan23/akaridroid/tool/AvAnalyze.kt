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
        MediaMetadataRetriever().compatUse { mediaMetadataRetriever ->
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
        MediaMetadataRetriever().compatUse { mediaMetadataRetriever ->
            when (ioType) {
                is IoType.AndroidUri -> mediaMetadataRetriever.setDataSource(context, ioType.uri)
                is IoType.JavaFile -> mediaMetadataRetriever.setDataSource(ioType.file.path)
            }
            val rotation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: return@withContext null
            val width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: return@withContext null
            val height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: return@withContext null
            val durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: return@withContext null
            val trackCount = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)?.toInt() ?: return@withContext null

            // 縦動画の場合、rotation で開店情報が入っていれば width / height を入れ替える
            val size = when (rotation) {
                90, 270 -> AvAnalyzeResult.Size(height, width)
                else -> AvAnalyzeResult.Size(width, height)
            }

            AvAnalyzeResult.Video(
                size = size,
                durationMs = durationMs,
                hasAudioTrack = trackCount == 2
            )
        }
    }

    /** [MediaMetadataRetriever]に[AutoCloseable]が実装されたのは Android 10 以降から。下位互換性つき use { } */
    private inline fun <R> MediaMetadataRetriever.compatUse(block: (MediaMetadataRetriever) -> R): R {
        // AutoCloseable を実装したのは Android 10 以降
        return if (this is AutoCloseable) {
            this.use(block)
        } else {
            val result = block(this)
            this.close()
            return result
        }
    }

}