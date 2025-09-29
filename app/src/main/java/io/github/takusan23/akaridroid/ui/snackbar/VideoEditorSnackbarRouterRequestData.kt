package io.github.takusan23.akaridroid.ui.snackbar

import android.net.Uri

sealed interface VideoEditorSnackbarRouterRequestData {

    /**
     * 画像として保存した
     *
     * @param uri 保存先
     */
    data class SaveVideoFrame(val uri: Uri) : VideoEditorSnackbarRouterRequestData

}