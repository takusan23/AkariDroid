package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.screen.about.AboutScreenUiState
import java.text.SimpleDateFormat
import java.util.Locale

// このアプリについて画面で使っている UI コンポーネント

/**
 * ルート選択というか選択肢画面
 *
 * @param modifier [Modifier]
 * @param onRouteSelect 選択した[AboutScreenUiState.AdvScenario]
 */
@Composable
fun AdvRouteSelect(
    modifier: Modifier = Modifier,
    onRouteSelect: (AboutScreenUiState.AdvScenario) -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AboutScreenUiState.AdvScenario.entries.forEach {
                AdvRouteSelectItem(
                    scenario = it,
                    onClick = { onRouteSelect(it) }
                )
            }
        }
    }
}

/**
 * アイコン
 *
 * @param modifier [Modifier]
 */
@Composable
fun AdvHiroin(modifier: Modifier) {
    Icon(
        modifier = modifier.size(100.dp),
        painter = painterResource(id = R.drawable.akari_droid_icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
    )
}

/**
 * 文字が出ている部分
 *
 * @param modifier [Modifier]
 * @param text テキスト
 */
@Composable
fun AdvTextArea(
    modifier: Modifier = Modifier,
    text: String
) {
    val context = LocalContext.current
    val appVersion = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }

    Surface(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "${stringResource(id = R.string.app_name)} ( $appVersion ) ",
                fontSize = 20.sp
            )
            HorizontalDivider()
            Text(
                text = text,
                fontSize = 20.sp
            )
        }
    }
}

/**
 * メニューバーみたいなやつ
 *
 * @param modifier [Modifier]
 * @param onClick おしたとき
 */
@Composable
fun AdvMenuBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = CutCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        onClick = onClick
    ) {
        Text(
            modifier = Modifier
                .padding(10.dp)
                .padding(end = 10.dp),
            text = "SKIP"
        )
    }
}

/**
 * 日付の部分
 *
 * @param modifier [Modifier]
 */
@Composable
fun AdvDateView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val simpleDateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val dateText = remember { simpleDateFormat.format(context.getString(R.string.build_date).toLong()) }

    Surface(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = CircleShape,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = dateText,
                fontSize = 20.sp
            )
        }
    }
}

/**
 * 選択肢の各アイテム
 *
 * @param modifier [Modifier]
 * @param scenario 選択肢
 * @param onClick 押した時
 */
@Composable
private fun AdvRouteSelectItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    scenario: AboutScreenUiState.AdvScenario
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            text = scenario.title
        )
    }
}
