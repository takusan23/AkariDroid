package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/** あかりんく画面 */
@Composable
fun AkaLinkBottomSheet() {
    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_folder_data_24px), contentDescription = null)
            Text(text = "あかりんく AkaLink 画面")
        }

        Text(
            text = """
            対応したアプリと連携して、素材を他のアプリから作成し、タイムラインに追加できます。
        """.trimIndent()
        )

        Button(onClick = { }) {
            Text(text = "アプリを開く")
        }
    }
}