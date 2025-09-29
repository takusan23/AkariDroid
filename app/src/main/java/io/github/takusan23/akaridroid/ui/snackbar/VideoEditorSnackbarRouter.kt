package io.github.takusan23.akaridroid.ui.snackbar

import android.content.Intent
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.takusan23.akaridroid.R
import kotlinx.coroutines.flow.collectLatest

/**
 * 動画編集画面で使う Snackbar のルーティング
 *
 * @param modifier [Modifier]
 * @param routerRequestData 表示したい Snackbar
 */
@Composable
fun VideoEditorSnackbarRouter(
    modifier: Modifier = Modifier,
    routerRequestData: VideoEditorSnackbarRouterRequestData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val requestState = rememberUpdatedState(routerRequestData)

    // リクエストに応じた Snackbar を出す
    LaunchedEffect(key1 = Unit) {
        snapshotFlow { requestState.value }.collectLatest { requestData ->
            try {
                when (requestData) {
                    is VideoEditorSnackbarRouterRequestData.SaveVideoFrame -> {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.video_edit_snackbar_save_frame_successful_title),
                            withDismissAction = true,
                            actionLabel = context.getString(R.string.video_edit_snackbar_save_frame_successful_button)
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, requestData.uri))
                        }
                    }
                }
            } finally {
                // キャンセルや表示終了時など
                onDismiss()
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    )
}