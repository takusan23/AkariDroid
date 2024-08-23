package io.github.takusan23.akaridroid.ui.bottomsheet.projectlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 * @param onDelete プロジェクト削除を押した時
 * @param onExport プロジェクトの持ち出しを押した時
 */
@Composable
fun ProjectMenuBottomSheet(
    onDelete: () -> Unit,
    onExport: () -> Unit
) {

    val isShowDeleteDialog = remember { mutableStateOf(false) }
    if (isShowDeleteDialog.value) {
        DeleteDialog(
            onDelete = onDelete,
            onDismiss = { isShowDeleteDialog.value = false }
        )
    }

    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = stringResource(id = R.string.project_list_bottomsheet_menu_title),
            fontSize = 24.sp
        )

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
            onClick = onExport
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
        confirmButton = {
            OutlinedButton(onClick = onDelete) {
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