package io.github.takusan23.akaridroid.ui.screen.about

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.AdvHiroin
import io.github.takusan23.akaridroid.ui.component.AdvMenuBar
import io.github.takusan23.akaridroid.ui.component.AdvRouteSelect
import io.github.takusan23.akaridroid.ui.component.AdvTextArea
import io.github.takusan23.akaridroid.ui.screen.NavigationPaths

private const val GitHubUrl = "https://github.com/takusan23/AkariDroid"
private const val AkariCoreUrl = "https://github.com/takusan23/AkariDroid/tree/master/akari-core"

/** このアプリについて画面の UI ステート */
sealed interface AboutScreenUiState {

    /** 初期状態 */
    data object Init : AboutScreenUiState

    /** ルート選択（選択肢） */
    data object RouteSelect : AboutScreenUiState

    /** ルート選択の選択肢 */
    enum class AdvScenario(val title: String) {
        Sushi("すし"),
        GitHub("GitHub をみる"),
        Library("動画編集ライブラリ")
    }
}

/** このアプリについて画面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigate: (NavigationPaths) -> Unit
) {
    val context = LocalContext.current
    // UI の状態
    val uiState = remember { mutableStateOf<AboutScreenUiState>(AboutScreenUiState.Init) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* TODO */ },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24px), contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (uiState.value) {
                AboutScreenUiState.Init -> InitScreen(
                    onClick = { uiState.value = AboutScreenUiState.RouteSelect }
                )

                AboutScreenUiState.RouteSelect -> RouteSelectScreen(
                    onRouteSelect = {
                        when (it) {
                            AboutScreenUiState.AdvScenario.Sushi -> onNavigate(NavigationPaths.SushiScreen)
                            AboutScreenUiState.AdvScenario.Library -> openBrowser(context, AkariCoreUrl)
                            AboutScreenUiState.AdvScenario.GitHub -> openBrowser(context, GitHubUrl)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InitScreen(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {

        AdvHiroin(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .weight(1f)
                .align(Alignment.CenterHorizontally)
        )

        AdvTextArea(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = "わたしは動画編集アプリだよ。よろしくね！"
        )

        AdvMenuBar(
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.End),
            onClick = onClick
        )
    }
}

@Composable
private fun RouteSelectScreen(onRouteSelect: (AboutScreenUiState.AdvScenario) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {

        AdvRouteSelect(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
                .weight(1f),
            onRouteSelect = onRouteSelect
        )

        AdvTextArea(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            text = "わたしは動画編集アプリだよ。よろしくね！"
        )

        AdvMenuBar(
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.End)
        )
    }
}

private fun openBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    context.startActivity(intent)
}