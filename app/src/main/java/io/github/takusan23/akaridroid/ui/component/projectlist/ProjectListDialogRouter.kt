package io.github.takusan23.akaridroid.ui.component.projectlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.projectlist.data.ProjectListDialogRequestData

/**
 * プロジェクト一覧画面でダイアログを出すルーター的なあれ
 *
 * @param dialogRequestData 出すダイアログ
 * @param onDelete プロジェクト削除
 * @param onDismiss ダイアログを消す
 */
@Composable
fun ProjectListDialogRouter(
    dialogRequestData: ProjectListDialogRequestData,
    onDismiss: () -> Unit,
    onDelete: (name: String) -> Unit,
) {
    when (dialogRequestData) {
        is ProjectListDialogRequestData.ProjectDeleteDialog -> DeleteDialog(
            onDelete = {
                onDelete(dialogRequestData.name)
                onDismiss()
            },
            onDismiss = onDismiss
        )

        is ProjectListDialogRequestData.ProjectExportDialog -> ExportOrImportProgressDialog(
            title = stringResource(id = R.string.project_list_dialog_export_title),
            message = stringResource(id = R.string.project_list_dialog_export_description),
            current = dialogRequestData.progress,
            total = dialogRequestData.totalCount
        )

        is ProjectListDialogRequestData.ProjectImportDialog -> ExportOrImportProgressDialog(
            title = stringResource(id = R.string.project_list_dialog_import_title),
            message = stringResource(id = R.string.project_list_dialog_import_description),
            current = dialogRequestData.progress,
            total = dialogRequestData.totalCount
        )
    }
}

/**
 * エクスポート、インポート中の進捗ダイアログ
 *
 * @param modifier [Modifier]
 * @param title タイトル
 * @param message 説明
 * @param current 現在の進捗
 * @param total 合計数
 */
@Composable
private fun ExportOrImportProgressDialog(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    current: Int,
    total: Int
) {
    Dialog(onDismissRequest = { /* 作業中は消させない */ }) {
        Card(modifier = modifier.fillMaxWidth(0.9f)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    painter = painterResource(id = R.drawable.ic_outline_business_center_24),
                    contentDescription = null
                )
                Text(
                    text = title,
                    fontSize = 24.sp
                )
                Text(
                    text = message,
                    fontSize = 14.sp
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { current / total.toFloat() }
                )
                Text(
                    modifier = Modifier.align(Alignment.End),
                    text = "$current / $total",
                    fontSize = 14.sp
                )
            }
        }
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
        title = { Text(text = stringResource(id = R.string.project_list_dialog_delete_title)) },
        icon = { Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null) },
        confirmButton = {
            Button(onClick = onDelete) {
                Text(text = stringResource(id = R.string.project_list_dialog_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.project_list_dialog_delete_cancel))
            }
        }
    )
}