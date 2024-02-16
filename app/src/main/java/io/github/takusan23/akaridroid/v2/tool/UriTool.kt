package io.github.takusan23.akaridroid.v2.tool

import android.content.Context
import android.content.Intent
import android.net.Uri

object UriTool {

    /** フォトピッカーとかで取り出した Uri を永続化する。アプリを再起動しても Uri が失効しないようにする */
    fun takePersistableUriPermission(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

}