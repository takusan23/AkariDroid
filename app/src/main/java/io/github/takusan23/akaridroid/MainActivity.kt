package io.github.takusan23.akaridroid

import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.github.takusan23.akaricore.AkariCore
import io.github.takusan23.akaricore.data.AudioEncoderData
import io.github.takusan23.akaricore.data.VideoEncoderData
import io.github.takusan23.akaricore.data.VideoFileData
import io.github.takusan23.akaridroid.ui.theme.AkariDroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val _state = MutableStateFlow(EncoderStatus.PREPARE)



        lifecycleScope.launch {
            val videoFile = File("${getExternalFilesDir(null)!!.path}/videos/demo.mp4")
            val resultFile = File(getExternalFilesDir(null), "result.mp4").apply {
                delete()
                createNewFile()
            }
            val tempFolder = File(getExternalFilesDir(null), "temp").apply { mkdir() }

            val videoEncoder = VideoEncoderData(codecName = MediaFormat.MIMETYPE_VIDEO_AVC)
            val audioEncoder = AudioEncoderData(codecName = MediaFormat.MIMETYPE_AUDIO_AAC)
            val videoFileData = VideoFileData(videoFile = videoFile, tempWorkFolder = tempFolder, containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4, outputFile = resultFile)

            val akariCore = AkariCore(videoFileData, videoEncoder, audioEncoder)
            _state.value = EncoderStatus.RUNNING
            withContext(Dispatchers.Default) {
                akariCore.start()
            }
            _state.value = EncoderStatus.FINISH
        }

        setContent {
            AkariDroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    val currentState = _state.collectAsState()
                    Text(text = currentState.value.name)
                }
            }
        }
    }

    private enum class EncoderStatus {
        PREPARE,
        RUNNING,
        FINISH,
    }

}