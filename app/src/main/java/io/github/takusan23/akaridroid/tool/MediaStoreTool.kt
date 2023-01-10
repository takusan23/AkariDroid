package io.github.takusan23.akaridroid.tool

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** MediaStore（端末のメディアフォルダーへ保存する仕組み）関連 */
object MediaStoreTool {

    /**
     * [File]から端末の動画フォルダへコピーする
     *
     * @param context [Context]
     * @param file コピーしたいファイルの[File]
     */
    suspend fun copyToVideoFolder(context: Context, file: File) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        // MediaStoreに入れる中身
        val contentValues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to file.name,
                MediaStore.MediaColumns.RELATIVE_PATH to "${Environment.DIRECTORY_MOVIES}/AkariDroid"
            )
        } else {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to file.name,
            )
        }
        // MediaStoreへ登録
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext
        contentResolver.openOutputStream(uri).use { outputStream -> outputStream?.write(file.readBytes()) }
    }

}