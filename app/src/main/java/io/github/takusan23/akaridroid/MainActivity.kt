package io.github.takusan23.akaridroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.takusan23.akaridroid.v2.ui.screen.AkariDroidMainScreenV2
import io.github.takusan23.akaridroid.v2.ui.theme.AkariDroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AkariDroidTheme {
                AkariDroidMainScreenV2()
            }
        }
    }
}