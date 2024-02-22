package io.github.takusan23.akaridroid.v2.tool

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import io.github.takusan23.akaridroid.v2.tool.data.AnalyzeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UriTool {

    /** フォトピッカーとかで取り出した Uri を永続化する。アプリを再起動しても Uri が失効しないようにする */
    fun takePersistableUriPermission(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /**
     * Uriのファイル名を取得する
     *
     * @param uri [Uri]
     * @return ファイル名。取れない場合は null
     */
    suspend fun getFileName(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        return@withContext context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media.DISPLAY_NAME), null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
    }

    /**
     * 画像を解析する
     *
     * @param uri フォトピッカーとかで
     * @param context [Context]
     * @return [AnalyzeResult.Image]
     */
    suspend fun analyzeImage(
        context: Context,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        val column = arrayOf(
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        context.contentResolver.query(uri, column, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            val width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
            val height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
            AnalyzeResult.Image(size = AnalyzeResult.Size(width, height))
        }
    }

    /**
     * 音声を解析する
     *
     * @param uri StorageAccessFramework とかで
     * @param context [Context]
     * @return [AnalyzeResult.Audio]
     */
    suspend fun analyzeAudio(
        context: Context,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        // MediaStore じゃ取れなかった
        MediaMetadataRetriever().use { mediaMetadataRetriever ->
            mediaMetadataRetriever.setDataSource(context, uri)
            val durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: return@withContext null
            AnalyzeResult.Audio(durationMs = durationMs)
        }
    }

    /**
     * 動画を解析する
     *
     * @param uri フォトピッカーとかで
     * @param context [Context]
     * @return [AnalyzeResult.Video]
     */
    suspend fun analyzeVideo(
        context: Context,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        // MediaStore じゃ取れなかった
        MediaMetadataRetriever().use { mediaMetadataRetriever ->
            mediaMetadataRetriever.setDataSource(context, uri)
            val width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: return@withContext null
            val height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: return@withContext null
            val durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: return@withContext null
            val trackCount = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)?.toInt() ?: return@withContext null

            AnalyzeResult.Video(
                size = AnalyzeResult.Size(width, height),
                durationMs = durationMs,
                hasAudioTrack = trackCount == 2
            )
        }
    }

}