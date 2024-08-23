package io.github.takusan23.akaridroid.ui.bottomsheet.projectlist

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable

/**
 * プロジェクト作成ボトムシート
 *
 * @param requestData 表示したいボトムシート
 * @param onDismiss 閉じたい時
 * @param onCreate プロジェクト作成時
 * @param onDelete プロジェクト削除時
 * @param onExport プロジェクトエクスポート時
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListBottomSheetRouter(
    requestData: ProjectListBottomSheetRequestData,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onExport: (String) -> Unit
) {
    ModalBottomSheet(
        windowInsets = WindowInsets(0, 0, 0, 0),
        onDismissRequest = onDismiss
    ) {

        when (requestData) {
            ProjectListBottomSheetRequestData.CreateNewProject -> CreateNewProjectBottomSheet(
                onCreate = {
                    onCreate(it)
                    onDismiss()
                }
            )

            is ProjectListBottomSheetRequestData.ProjectMenu -> ProjectMenuBottomSheet(
                onDelete = {
                    onDelete(requestData.name)
                    onDismiss()
                },
                onExport = {
                    onExport(requestData.name)
                    onDismiss()
                }
            )
        }
    }
}