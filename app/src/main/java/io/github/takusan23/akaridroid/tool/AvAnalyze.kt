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

        // Uri の場合は、MediaStore に問い合わせてみる
        // 早期 return
        if (ioType is IoType.AndroidUri) {
            val column = arrayOf(
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )
            val resultSize = context.contentResolver.query(ioType.uri, column, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                AvAnalyzeResult.Size(width, height)
            }

            // 0 とかが帰ってきた場合はおかしいので、Bitmap の解析へ進む
            // null も Bitmap 解析へ進む
            if (resultSize != null && 0 < resultSize.width && 0 < resultSize.height) {
                return@withContext AvAnalyzeResult.Image(size = AvAnalyzeResult.Size(resultSize.width, resultSize.height))
            }
        }

        // Bitmap 解析
        val bitmap = when (ioType) {
            is IoType.AndroidUri -> context.contentResolver.openInputStream(ioType.uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }

            is IoType.JavaFile -> BitmapFactory.decodeFile(ioType.file.path)
        } ?: return@withContext null // これでもダメなら null

        return@withContext AvAnalyzeResult.Image(size = AvAnalyzeResult.Size(bitmap.width, bitmap.height))
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
            val hasAudioTrack = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null

            // 縦動画の場合、rotation で開店情報が入っていれば width / height を入れ替える
            val size = when (rotation) {
                90, 270 -> AvAnalyzeResult.Size(height, width)
                else -> AvAnalyzeResult.Size(width, height)
            }

            AvAnalyzeResult.Video(
                size = size,
                durationMs = durationMs,
                hasAudioTrack = hasAudioTrack
            )
        }
    }

    /** [MediaMetadataRetriever]に[AutoCloseable]が実装されたのは Android 10 以降から。下位互換性つき use { } */
    private inline fun <R> MediaMetadataRetriever.compatUse(block: (MediaMetadataRetriever) -> R): R {
        // AutoCloseable を実装したのは Android 10 以降
        @Suppress("USELESS_IS_CHECK")
        return if (this is AutoCloseable) {
            this.use(block)
        } else {
            val result = block(this)
            this.close()
            return result
        }
    }

}