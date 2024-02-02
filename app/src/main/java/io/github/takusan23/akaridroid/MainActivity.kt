package io.github.takusan23.akaridroid

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.takusan23.akaridroid.ui.component.AkariDroidMainScreen
import kotlinx.coroutines.flow.MutableStateFlow

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
            AkariDroidMainScreen()
        }
    }

    private enum class EncoderStatus {
        PREPARE,
        RUNNING,
        FINISH,
    }

}