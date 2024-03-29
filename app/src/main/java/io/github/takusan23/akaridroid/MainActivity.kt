package io.github.takusan23.akaridroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.takusan23.akaridroid.ui.screen.AkariDroidMainScreen
import io.github.takusan23.akaridroid.ui.theme.AkariDroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // スプラッシュスクリーン
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AkariDroidTheme {
                AkariDroidMainScreen()
            }
        }
    }
}