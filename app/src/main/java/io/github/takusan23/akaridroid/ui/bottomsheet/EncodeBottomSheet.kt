package io.github.takusan23.akaridroid.ui.bottomsheet

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.encoder.EncoderParameters
import io.github.takusan23.akaridroid.ui.component.ExtendDropDownMenu
import io.github.takusan23.akaridroid.ui.component.OutlinedIntTextField
import io.github.takusan23.akaridroid.ui.component.data.ExtendDropDownMenuItem

/** コンテナフォーマットの説明 */
private val ContainerFormatMenu = listOf(
    ExtendDropDownMenuItem("MP4", ".mp4 ファイルです。AVC / HEVC / AV1 / AAC コーデックが格納できます。"),
    ExtendDropDownMenuItem("WebM", ".webm ファイルです。VP9 / Opus コーデックが格納できます。")
)

/** 音声コーデックの説明 */
private val AudioCodecMenu = listOf(
    ExtendDropDownMenuItem("AAC", "mp4 コンテナ用"),
    ExtendDropDownMenuItem("Opus", "WebM コンテナ用")
)

/** 映像コーデックの説明 */
private val VideoCodecMenu = listOfNotNull(
    ExtendDropDownMenuItem("AVC（H.264）", "再生できる端末が一番多いです。高画質にしたい場合はビットレートを結構上げないといけない。とりあえずこれにしておいけばいいはず。"),
    ExtendDropDownMenuItem("HEVC（H.265）", "AVC より効率が良いですが、特許問題があるため使っていいのか不明。法律に詳しくなく分かりません。"),
    ExtendDropDownMenuItem("VP9", "WebM コンテナ用。"),
    // AV1 エンコードは Android 14 以降のみ
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ExtendDropDownMenuItem("AV1", "HEVC と同等の性能と言われており、かつロイヤリティフリーです。しかし、ほとんどの端末にハードウェアエンコーダーが搭載されていないため、エンコードに相当な時間がかかります。")
    } else null
)

/**
 * 動画の保存画面。エンコード画面
 * TODO バリデーションやってあげたほうが親切かも（コーデックとコンテナ対応しているかとか）
 *
 * @param onEncode エンコードを押した時に呼ばれる
 */
@Composable
fun EncodeBottomSheet(
    onEncode: (String, EncoderParameters) -> Unit
) {
    // とりあえず高画質で
    val encoderParameters = remember { mutableStateOf(EncoderParameters.HIGH_QUALITY) }
    val fileName = remember { mutableStateOf("あかりどろいど_${System.currentTimeMillis()}") }

    fun update(copy: (EncoderParameters.AudioVideo) -> EncoderParameters.AudioVideo) {
        encoderParameters.value = copy(encoderParameters.value)
    }

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Column {
                Text(
                    text = "動画の保存（エンコード）",
                    fontSize = 24.sp
                )
                Text(text = "よくわからない場合は変更しないでそのまま保存（エンコード）したほうがいいと思います。")
            }

            // ファイル名
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "ファイル名") },
                suffix = { Text(text = ".${encoderParameters.value.containerFormat.extension}") },
                value = fileName.value,
                onValueChange = { fileName.value = it }
            )

            // 映像コーデック設定
            VideoEncoderSetting(
                videoEncoderParameters = encoderParameters.value.videoEncoderParameters,
                onUpdate = { videoEncoderParameters -> update { it.copy(videoEncoderParameters = videoEncoderParameters) } }
            )

            // 音声コーデック設定
            AudioEncoderSetting(
                audioEncoderParameters = encoderParameters.value.audioEncoderParameters,
                onUpdate = { audioEncoderParameters -> update { it.copy(audioEncoderParameters = audioEncoderParameters) } }
            )

            // コンテナフォーマット設定
            ContainerFormatSetting(
                containerFormat = encoderParameters.value.containerFormat,
                onUpdate = { containerFormat -> update { it.copy(containerFormat = containerFormat) } }
            )

            // エンコード設定を見れるように
            EncoderParametersLog(encoderParameters = encoderParameters.value)

            // ないとスクロールがぎこちない
            Spacer(modifier = Modifier.height(50.dp))
        }

        HorizontalDivider()
        Row(modifier = Modifier.padding(10.dp)) {
            Text(
                modifier = Modifier.weight(1f),
                text = "保存（エンコード）にはしばらく時間がかかります"
            )
            Button(
                onClick = {
                    onEncode(fileName.value, encoderParameters.value)
                }
            ) {
                Text(text = "保存を開始")
            }
        }
    }
}

/** エンコード設定を文字列で見れるように */
@Composable
private fun EncoderParametersLog(encoderParameters: EncoderParameters) {
    val context = LocalContext.current
    val toString = encoderParameters.toString()

    OutlinedCard {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "エンコード設定（調査用）")

            Text(text = toString)

            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        setType("text/plain")
                        putExtra(Intent.EXTRA_TEXT, toString)
                    }
                    context.startActivity(intent)
                }
            ) {
                Text(text = "共有")
            }
        }
    }
}

/** コンテナフォーマットの設定 */
@Composable
private fun ContainerFormatSetting(
    containerFormat: EncoderParameters.ContainerFormat,
    onUpdate: (EncoderParameters.ContainerFormat) -> Unit
) {
    val isOpen = remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "コンテナフォーマット",
            fontSize = 20.sp
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(10.dp),
                text = "ここで mp4 を選択しても、再生可否はプレイヤーが対応しているコーデック次第なので、注意してね。（無いと思うけど）"
            )
        }

        ExtendDropDownMenu(
            modifier = Modifier.fillMaxWidth(),
            isOpen = isOpen.value,
            label = "コンテナフォーマット",
            iconResId = R.drawable.ic_outline_video_file_24,
            selectIndex = EncoderParameters.ContainerFormat.entries.indexOf(containerFormat),
            menuList = ContainerFormatMenu,
            onOpenChange = { isOpen.value = !isOpen.value },
            onSelect = { index -> onUpdate(EncoderParameters.ContainerFormat.entries[index]) }
        )
    }
}

/** 音声エンコーダーの設定 */
@Composable
private fun AudioEncoderSetting(
    audioEncoderParameters: EncoderParameters.AudioEncoderParameters,
    onUpdate: (EncoderParameters.AudioEncoderParameters) -> Unit
) {
    val isOpen = remember { mutableStateOf(false) }

    fun update(copy: (EncoderParameters.AudioEncoderParameters) -> EncoderParameters.AudioEncoderParameters) {
        onUpdate(copy(audioEncoderParameters))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "音声トラック",
            fontSize = 20.sp
        )

        // コンテナ
        ExtendDropDownMenu(
            modifier = Modifier.fillMaxWidth(),
            isOpen = isOpen.value,
            label = "音声コーデック",
            iconResId = R.drawable.ic_outline_audiotrack_24,
            selectIndex = EncoderParameters.AudioCodec.entries.indexOf(audioEncoderParameters.codec),
            menuList = AudioCodecMenu,
            onOpenChange = { isOpen.value = !isOpen.value },
            onSelect = { index -> update { it.copy(codec = EncoderParameters.AudioCodec.entries[index]) } }
        )

        // ビットレート
        OutlinedIntTextField(
            modifier = Modifier.fillMaxWidth(),
            value = audioEncoderParameters.bitrate,
            onValueChange = { bitrate -> update { it.copy(bitrate = bitrate) } },
            label = { Text(text = "音声ビットレート") }
        )
    }
}

/** 映像エンコーダーの設定 */
@Composable
private fun VideoEncoderSetting(
    videoEncoderParameters: EncoderParameters.VideoEncoderParameters,
    onUpdate: (EncoderParameters.VideoEncoderParameters) -> Unit
) {
    val isOpen = remember { mutableStateOf(false) }

    fun update(copy: (EncoderParameters.VideoEncoderParameters) -> EncoderParameters.VideoEncoderParameters) {
        onUpdate(copy(videoEncoderParameters))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text(
            text = "映像トラック",
            fontSize = 20.sp
        )

        ExtendDropDownMenu(
            modifier = Modifier.fillMaxWidth(),
            isOpen = isOpen.value,
            label = "映像コーデック",
            iconResId = R.drawable.ic_outline_video_file_24,
            selectIndex = EncoderParameters.VideoCodec.entries.indexOf(videoEncoderParameters.codec),
            menuList = VideoCodecMenu,
            onOpenChange = { isOpen.value = !isOpen.value },
            onSelect = { index -> update { it.copy(codec = EncoderParameters.VideoCodec.entries[index]) } }
        )

        // ビットレート
        OutlinedIntTextField(
            modifier = Modifier.fillMaxWidth(),
            value = videoEncoderParameters.bitrate,
            onValueChange = { bitrate -> update { it.copy(bitrate = bitrate) } },
            label = { Text(text = "映像ビットレート") }
        )

        // フレームレート
        OutlinedIntTextField(
            modifier = Modifier.fillMaxWidth(),
            value = videoEncoderParameters.frameRate,
            onValueChange = { frameRate -> update { it.copy(frameRate = frameRate) } },
            label = { Text(text = "フレームレート") }
        )

        // キーフレーム間隔
        OutlinedIntTextField(
            modifier = Modifier.fillMaxWidth(),
            value = videoEncoderParameters.keyframeInterval,
            onValueChange = { keyframeInterval -> update { it.copy(keyframeInterval = keyframeInterval) } },
            label = { Text(text = "キーフレーム間隔（秒）") }
        )

    }
}