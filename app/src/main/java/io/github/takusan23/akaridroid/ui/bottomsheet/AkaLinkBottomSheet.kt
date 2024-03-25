package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.tool.AkaLinkTool
import kotlinx.coroutines.launch

/**
 * あかりんく画面
 *
 * @param onAkaLinkResult あかりんく（外部連携）が終わった時に呼ばれる
 */
@Composable
fun AkaLinkBottomSheet(onAkaLinkResult: (AkaLinkTool.AkaLinkResult) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var startIntentData = remember<AkaLinkTool.AkaLinkIntentData?> { null }
    val activityResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            scope.launch {
                // 処理してもらう
                val akaLinkResult = AkaLinkTool.resolveAkaLinkResultIntent(
                    context = context,
                    resultCode = it.resultCode,
                    resultIntent = it.data,
                    akaLinkIntentData = startIntentData
                )
                if (akaLinkResult != null) {
                    onAkaLinkResult(akaLinkResult)
                }
            }
        }
    )

    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_folder_data_24px), contentDescription = null)
            Text(
                text = "あかりんく（AkaLink）画面",
                fontSize = 24.sp
            )
        }

        Text(
            text = """
            対応したアプリと連携して、素材を他のアプリから作成し、タイムラインに追加できます。
        """.trimIndent()
        )

        Button(onClick = {
            val akaLinkIntentData = AkaLinkTool.createAkaLinkStartIntent(context)
            startIntentData = akaLinkIntentData
            activityResult.launch(akaLinkIntentData.intent)
        }) {
            Text(text = "アプリを開く")
        }
    }
}