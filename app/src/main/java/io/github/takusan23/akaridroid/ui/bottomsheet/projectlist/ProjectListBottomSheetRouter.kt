package io.github.takusan23.akaridroid.ui.bottomsheet.projectlist

import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable

/**
 * プロジェクト作成ボトムシート
 *
 * @param requestData 表示したいボトムシート
 * @param onDismiss 閉じたい時
 * @param onCreate プロジェクト作成時
 * @param onDeleteMenuClick プロジェクト削除メニューを押したとき
 * @param onExportMenuClick エクスポートメニューを押したとき。名前とエクスポート先 Uri
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListBottomSheetRouter(
    requestData: ProjectListBottomSheetRequestData,
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit,
    onDeleteMenuClick: (name: String) -> Unit,
    onExportMenuClick: (name: String, Uri) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {

        when (requestData) {
            ProjectListBottomSheetRequestData.CreateNewProject -> CreateNewProjectBottomSheet(
                onCreate = {
                    onCreate(it)
                    onDismiss()
                }
            )

            is ProjectListBottomSheetRequestData.ProjectMenu -> ProjectMenuBottomSheet(
                name = requestData.name,
                onDeleteMenuClick = {
                    onDeleteMenuClick(it)
                    onDismiss()
                },
                onExportMenuClick = { name, uri ->
                    onExportMenuClick(name, uri)
                    onDismiss()
                }
            )
        }
    }
}