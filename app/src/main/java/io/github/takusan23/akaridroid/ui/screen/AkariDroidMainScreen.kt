package io.github.takusan23.akaridroid.ui.component

import androidx.compose.runtime.Composable
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

    AkariDroidTheme {
        NavHost(navController = navController, startDestination = NavigationPaths.VideoEditor.path) {
            composable(NavigationPaths.Home.path) {
                HomeScreen(onNavigate = { navController.navigate(it) })
            }
            composable(NavigationPaths.CreateVideo.path) {
                CreateVideoScreen()
            }
            composable(NavigationPaths.VideoEditor.path) {
                VideoEditorScreen()
            }
        }
    }
}