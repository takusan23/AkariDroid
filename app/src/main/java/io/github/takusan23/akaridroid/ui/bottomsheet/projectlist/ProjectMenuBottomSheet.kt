package io.github.takusan23.akaridroid.ui.bottomsheet.projectlist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.bottomsheet.bottomSheetPadding
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem

/**
 * プロジェクト一覧画面のメニュー用ボトムシート
 *
 * @param onDelete プロジェクト削除メニューを押した時
 * @param onExport プロジェクトの持ち出しメニューを押した時
 */
@Composable
fun ProjectMenuBottomSheet(
    name: String,
    onDeleteMenuClick: (name: String) -> Unit,
    onExportMenuClick: (name: String, Uri) -> Unit
) {

    // zip ファイル名
    val portableProjectName = "${name}_portable.zip"

    // application/zip 多分無理
    val createZipFile = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        onExportMenuClick(name, uri)
    }

    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Column {
            Text(
                text = name,
                fontSize = 24.sp
            )
            Text(text = stringResource(id = R.string.project_list_bottomsheet_menu_title))
        }

        BottomSheetMenuItem(
            title = stringResource(id = R.string.project_list_bottomsheet_menu_delete_title),
            description = stringResource(id = R.string.project_list_bottomsheet_menu_delete_description),
            iconResId = R.drawable.ic_outline_delete_24px,
            onClick = { onDeleteMenuClick(name) }
        )

        BottomSheetMenuItem(
            title = stringResource(id = R.string.project_list_bottomsheet_menu_export_title),
            description = stringResource(id = R.string.project_list_bottomsheet_menu_export_description),
            iconResId = R.drawable.ic_outline_business_center_24,
            onClick = {
                // 保存先を選んでもらう
                createZipFile.launch(portableProjectName)
            }
        )
    }
}
