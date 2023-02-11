package io.github.takusan23.akaridroid.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.takusan23.akaridroid.ui.screen.CreateVideoScreen
import io.github.takusan23.akaridroid.ui.screen.NavigationPaths
import io.github.takusan23.akaridroid.ui.screen.VideoEditorScreen
import io.github.takusan23.akaridroid.ui.theme.AkariDroidTheme
import io.github.takusan23.akaridroid.ui.tool.SetNavigationBarColor
import io.github.takusan23.akaridroid.ui.tool.SetStatusBarColor

/** メイン画面。この中でルーティングしている */
@Composable
fun AkariDroidMainScreen() {

    // 画面遷移
    val navController = rememberNavController()

    SetStatusBarColor()
    SetNavigationBarColor()

    // startDestination にパラメーターつけると動かなくなるので
    // 今は編集画面メインで作ってるので即時で編集画面に飛ばすように
    LaunchedEffect(key1 = Unit) {
        navController.navigate(NavigationPaths.createEditorPath("project-2022-01-10"))
    }

    AkariDroidTheme {
        NavHost(navController = navController, startDestination = NavigationPaths.Home.path) {
            composable(NavigationPaths.Home.path) {
                HomeScreen(onNavigate = { navController.navigate(it) })
            }
            composable(NavigationPaths.CreateVideo.path) {
                CreateVideoScreen()
            }
            composable(NavigationPaths.VideoEditor.path) {
                // パラメーターは ViewModel 側で受け取れる
                VideoEditorScreen(viewModel = viewModel())
            }
        }
    }
}