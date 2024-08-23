package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/**
 * プロジェクト追加、取り込みボタン
 *
 * @param modifier [Modifier]
 * @param onCreate 作成ボタン押した時
 * @param onImport 取り込みボタン押した時
 */
@Composable
fun ProjectListMenu(
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
    onImport: () -> Unit
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Button(onClick = onCreate) {
            Icon(painter = painterResource(id = R.drawable.ic_outlined_add_24px), contentDescription = null)
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.project_list_create_project))
        }

        OutlinedButton(onClick = onImport) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_business_center_24), contentDescription = null)
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.project_list_import_project))
        }
    }
}