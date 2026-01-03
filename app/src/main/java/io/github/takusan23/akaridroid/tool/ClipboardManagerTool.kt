package io.github.takusan23.akaridroid.tool

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * [ClipboardManager] をたたくだけ
 *
 * Koin DI ライブラリ経由でこのクラスのインスタンスが取得できます
 *
 * @param context Koin 経由で
 */
class ClipboardManagerTool(private val context: Context) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    /** [ClipboardManager.getPrimaryClip] を呼び出す */
    val primaryClip
        get() = clipboardManager.primaryClip

    /** [ClipboardManager.setPrimaryClip] を呼び出す */
    fun setPrimaryClip(clipData: ClipData) = clipboardManager.setPrimaryClip(clipData)

}