package io.github.takusan23.akaridroid

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.takusan23.akaricore.v2.video.VideoFrameBitmapExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val paint = Paint().apply {
        color = Color.WHITE
        textSize = 80f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val _state = MutableStateFlow(EncoderStatus.PREPARE)

        /*
                lifecycleScope.launch {
                    val videoFile = File("${getExternalFilesDir(null)!!.path}/videos/sample.mp4")
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
                        // エンコーダーを開始する
                        akariCore.start { positionMs ->
                            // this は Canvas
                            // 動画の上に重ねるCanvasを描画する
                            // drawColor(Color.parseColor("#40FFFFFF"))
                            drawText("再生時間 = ${"%.02f".format((positionMs / 1000F))} 秒", 50f, 80f, paint)
                        }
                    }
                    _state.value = EncoderStatus.FINISH
                }
        */

        setContent {
            val scope = rememberCoroutineScope()
            val composeBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
            val videoFrameBitmapExtractor = remember { VideoFrameBitmapExtractor() }
            val currentPositionMs = remember { mutableLongStateOf(3000) }

            fun nextFrame() {
                scope.launch {
                    currentPositionMs.longValue += 16
                    val bitmap = videoFrameBitmapExtractor.getVideoFrameBitmap(currentPositionMs.longValue)
                    composeBitmap.value = bitmap.asImageBitmap()
                }
            }

            DisposableEffect(key1 = Unit) {
                scope.launch {
                    val videoFile = getExternalFilesDir(null)!!.resolve("apple.ts")
                    videoFrameBitmapExtractor.prepareDecoder(videoFile)
                    nextFrame()
                }
                onDispose { videoFrameBitmapExtractor.destroy() }
            }


            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (composeBitmap.value != null) {
                    Image(bitmap = composeBitmap.value!!, contentDescription = null)
                }
                Text(text = "${currentPositionMs.value} ms")
                Button(onClick = { nextFrame() }) {
                    Text(text = "進める")
                }
            }

            // AkariDroidMainScreen()
        }
    }

    private enum class EncoderStatus {
        PREPARE,
        RUNNING,
        FINISH,
    }

}