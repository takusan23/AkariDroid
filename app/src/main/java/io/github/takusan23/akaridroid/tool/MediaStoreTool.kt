package io.github.takusan23.akaridroid.tool

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MediaStore（端末のメディアフォルダーへ保存する仕組み）関連
 *
 * Koin DI ライブラリ経由でこのクラスのインスタンスが取得できます
 *
 * @param context Koin 経由で
 */
class MediaStoreTool(private val context: Context) {

    /**
     * Uriのファイル名を取得する
     *
     * @param uri [Uri]
     * @return ファイル名。取れない場合は null
     */
    suspend fun getFileName(uri: Uri) = withContext(Dispatchers.IO) {
        return@withContext context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media.DISPLAY_NAME), null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
    }

    /**
     * ファイルをコピーする
     *
     * @param uri [Uri]
     * @param copyTo コピー先
     */
    suspend fun fileCopy(uri: Uri, copyTo: File) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            copyTo.outputStream().use { output ->
                input.copyTo(output, FILE_COPY_BUFFER_SIZE)
            }
        }
    }

    /**
     * [File]から端末の動画フォルダへコピーする
     *
     * @param file コピーしたいファイルの[File]
     */
    suspend fun copyToVideoFolder(file: File) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        // MediaStoreに入れる中身
        val contentValues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to file.name,
                MediaStore.MediaColumns.RELATIVE_PATH to "${Environment.DIRECTORY_MOVIES}/AkariDroid",
                MediaStore.MediaColumns.MIME_TYPE to "video/mp4"
            )
        } else {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to file.name,
                MediaStore.MediaColumns.MIME_TYPE to "video/mp4"
            )
        }
        // MediaStoreへ登録
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    /**
     * [Bitmap]を写真フォルダに保存する
     *
     * @param bitmap 保存したい[Bitmap]
     */
    suspend fun saveBitmapToPictureFolder(
        bitmap: Bitmap,
        fileName: String = "akaridroid_image_${System.currentTimeMillis()}.jpg"
    ): Uri? = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        // MediaStoreに入れる中身
        val contentValues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to fileName,
                MediaStore.MediaColumns.RELATIVE_PATH to "${Environment.DIRECTORY_PICTURES}/AkariDroid"
            )
        } else {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to fileName
            )
        }
        // MediaStoreへ登録
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext null
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }
        uri
    }

    /**
     * [File]から端末の音声フォルダへコピーする
     *
     * @param file コピーしたいファイルの[File]
     */
    suspend fun copyToAudioFolder(file: File) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        // MediaStoreに入れる中身
        val contentValues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to file.name,
                MediaStore.MediaColumns.RELATIVE_PATH to "${Environment.DIRECTORY_MUSIC}/AkariDroid",
                MediaStore.MediaColumns.MIME_TYPE to "audio/aac"
            )
        } else {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to file.name,
                MediaStore.MediaColumns.MIME_TYPE to "audio/aac"
            )
        }
        // MediaStoreへ登録
        val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    /**
     * Uri の MIME-Type を取得する
     *
     * @param uri [Uri]
     * @return MIME-Type
     */
    suspend fun getMimeType(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.getType(uri)
    }

    /** akari-core の [toAkariCoreInputOutputData] を呼び出す。Context が必要なので、、、 */
    fun Uri.toInvokeAkariCoreInputOutputData() = this.toAkariCoreInputOutputData(context)

    companion object {

        /** コピー時のバッファサイズ */
        private const val FILE_COPY_BUFFER_SIZE = 8 * 1024
    }
}