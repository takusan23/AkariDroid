package io.github.takusan23.akaridroid.ui.bottomsheet.projectlist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.bottomsheet.bottomSheetPadding
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem

/**
 * プロジェクト一覧画面のメニュー用ボトムシート
 *
 * @param onDelete プロジェクト削除を押した時
 * @param onExport プロジェクトの持ち出しを押した時
 */
@Composable
fun ProjectMenuBottomSheet(
    name: String,
    onDelete: (String) -> Unit,
    onExport: (name: String, portableName: String, Uri) -> Unit
) {

    // zip ファイル名
    val portableProjectName = "${name}_portable.zip"

    val isShowDeleteDialog = remember { mutableStateOf(false) }
    if (isShowDeleteDialog.value) {
        DeleteDialog(
            onDelete = { onDelete(name) },
            onDismiss = { isShowDeleteDialog.value = false }
        )
    }

    // application/zip 多分無理
    val createZipFile = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        onExport(name, portableProjectName, uri)
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
            onClick = { isShowDeleteDialog.value = true } // ダイアログで本当に削除するか
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

/** 削除ダイアログ */
@Composable
private fun DeleteDialog(
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.project_list_bottomsheet_menu_delete_dialog_title)) },
        icon = { Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null) },
        confirmButton = {
            Button(onClick = onDelete) {
                Text(text = stringResource(id = R.string.project_list_bottomsheet_menu_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.project_list_bottomsheet_menu_delete_dialog_cancel))
            }
        }
    )
}