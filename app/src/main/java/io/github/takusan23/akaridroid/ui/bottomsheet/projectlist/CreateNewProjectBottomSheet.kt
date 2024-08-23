package io.github.takusan23.akaridroid.ui.bottomsheet.projectlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.bottomsheet.bottomSheetPadding

/**
 * プロジェクト一覧画面のボトムシート
 *
 * @param onCreate 作成ボタン押した時。引数は名前
 */
@Composable
fun CreateNewProjectBottomSheet(onCreate: (String) -> Unit) {
    val name = remember { mutableStateOf("") }

    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = stringResource(id = R.string.project_list_bottomsheet_create_title),
            fontSize = 24.sp
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text(text = stringResource(id = R.string.project_list_bottomsheet_create_name)) },
            singleLine = true
        )

        Button(
            modifier = Modifier.align(Alignment.End),
            onClick = { onCreate(name.value) }
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_outlined_add_24px), contentDescription = null)
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(id = R.string.project_list_bottomsheet_create_button))
        }
    }
}