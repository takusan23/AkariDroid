package io.github.takusan23.akaridroid.v2.tool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
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

}