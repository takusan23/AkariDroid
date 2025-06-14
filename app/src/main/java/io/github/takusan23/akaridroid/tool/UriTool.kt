package io.github.takusan23.akaridroid.tool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UriTool {

    /** フォトピッカーとかで取り出した Uri を永続化する。アプリを再起動しても Uri が失効しないようにする */
    fun takePersistableUriPermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // revoke の方でやってるのでこっちでも
        }
    }

    /** 永続化した Uri を解除する。多分必要。 */
    fun revokePersistableUriPermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            // java.lang.SecurityException: No permission grants found for UID
            // よくわからない
        }
    }

    /**
     * [Uri]が存在するかを返す
     *
     * @param context [Context]
     * @param uri 真偽が不明な[Uri]
     * @return 存在すれば true
     */
    suspend fun existsContentUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        // Uri が存在するかを返す処理はない
        // 仕方ないので try-catch で開けるか試す。あれば true のハズ。
        runCatching {
            context.contentResolver.openInputStream(uri).use { /* 存在しない、Uri が期限切れの場合は例外 */ }
        }.isSuccess
    }

    /**
     * Uriのファイル名を取得する
     *
     * @param uri [Uri]
     * @return ファイル名。取れない場合は null
     */
    suspend fun getFileName(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media.DISPLAY_NAME), null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                cursor.getString(0)
            }
        }.getOrNull()
    }
}