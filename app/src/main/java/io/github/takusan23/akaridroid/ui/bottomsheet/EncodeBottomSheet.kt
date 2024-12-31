package io.github.takusan23.akaridroid.ui.bottomsheet

import android.content.Intent
import android.os.Build
import android.os.Environment
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaricore.audio.AkariCoreAudioProperties
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.encoder.EncoderParameters
import io.github.takusan23.akaridroid.tool.NumberFormat
import io.github.takusan23.akaridroid.ui.component.ExtendMenu
import io.github.takusan23.akaridroid.ui.component.ExtendMenuItem
import io.github.takusan23.akaridroid.ui.component.MessageCard
import io.github.takusan23.akaridroid.ui.component.NoOpenableExtendMenu
import io.github.takusan23.akaridroid.ui.component.OutlinedIntTextField

/** コンテナフォーマットの説明 */
private val ContainerFormatMenu = listOf(
    Triple(EncoderParameters.ContainerFormat.MP4, R.string.video_edit_bottomsheet_encode_container_format_mp4_title, R.string.video_edit_bottomsheet_encode_container_format_mp4_description),
    Triple(EncoderParameters.ContainerFormat.WEBM, R.string.video_edit_bottomsheet_encode_container_format_webm_title, R.string.video_edit_bottomsheet_encode_container_format_webm_description)
)

/** 音声コーデックの説明 */
private val AudioCodecMenu = listOf(
    Triple(EncoderParameters.AudioCodec.AAC, R.string.video_edit_bottomsheet_encode_audio_encoder_aac_title, R.string.video_edit_bottomsheet_encode_audio_encoder_aac_description),
    Triple(EncoderParameters.AudioCodec.OPUS, R.string.video_edit_bottomsheet_encode_audio_encoder_opus_title, R.string.video_edit_bottomsheet_encode_audio_encoder_opus_description)
)

/**
 * 映像コーデックの説明を返す
 * 10-bit HDR が true の場合は、HEVC と AV1 のみ返します。
 *
 * @param isTenBitHdr 10-bit HDR が有効の場合は true
 */
private fun getVideoCodecMenu(isTenBitHdr: Boolean) = if (isTenBitHdr) listOfNotNull(
    Triple(EncoderParameters.VideoCodec.HEVC, R.string.video_edit_bottomsheet_encode_video_encoder_video_hevc_title, R.string.video_edit_bottomsheet_encode_video_encoder_video_hevc_description),
    // AV1 エンコードは Android 14 以降のみ
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Triple(EncoderParameters.VideoCodec.AV1, R.string.video_edit_bottomsheet_encode_video_encoder_video_av1_title, R.string.video_edit_bottomsheet_encode_video_encoder_video_av1_description)
    } else null
) else listOfNotNull(
    Triple(EncoderParameters.VideoCodec.AVC, R.string.video_edit_bottomsheet_encode_video_encoder_video_avc_title, R.string.video_edit_bottomsheet_encode_video_encoder_video_avc_description),
    Triple(EncoderParameters.VideoCodec.HEVC, R.string.video_edit_bottomsheet_encode_video_encoder_video_hevc_title, R.string.video_edit_bottomsheet_encode_video_encoder_video_hevc_description),
    Triple(EncoderParameters.VideoCodec.VP9, R.string.video_edit_bottomsheet_encode_video_encoder_video_vp9_title, R.string.video_edit_bottomsheet_encode_video_encoder_video_vp9_description),
    // AV1 エンコードは Android 14 以降のみ
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Triple(EncoderParameters.VideoCodec.AV1, R.string.video_edit_bottomsheet_encode_video_encoder_video_av1_title, R.string.video_edit_bottomsheet_encode_video_encoder_video_av1_description)
    } else null
)

/**
 * エンコード設定のプリセットを返す
 *
 * @param isTenBitHdr 10-bit HDR が有効の場合は true
 * @return プリセット一覧
 */
private fun getParametersPresetList(isTenBitHdr: Boolean) = if (isTenBitHdr) listOf(
    Triple(EncoderParameters.TEN_BIT_HDR_LOW_QUALITY, R.string.video_edit_bottomsheet_encode_basic_low_title, R.string.video_edit_bottomsheet_encode_basic_ten_bit_hdr_low_description),
    Triple(EncoderParameters.TEN_BIT_HDR_MEDIUM_QUALITY, R.string.video_edit_bottomsheet_encode_basic_medium_title, R.string.video_edit_bottomsheet_encode_basic_ten_bit_hdr_medium_description),
    Triple(EncoderParameters.TEN_BIT_HDR_HIGH_QUALITY, R.string.video_edit_bottomsheet_encode_basic_high_title, R.string.video_edit_bottomsheet_encode_basic_ten_bit_hdr_high_description)
) else listOf(
    Triple(EncoderParameters.LOW_QUALITY, R.string.video_edit_bottomsheet_encode_basic_low_title, R.string.video_edit_bottomsheet_encode_basic_low_description),
    Triple(EncoderParameters.MEDIUM_QUALITY, R.string.video_edit_bottomsheet_encode_basic_medium_title, R.string.video_edit_bottomsheet_encode_basic_medium_description),
    Triple(EncoderParameters.HIGH_QUALITY, R.string.video_edit_bottomsheet_encode_basic_high_title, R.string.video_edit_bottomsheet_encode_basic_high_description)
)

/**
 * セグメントボタン？で切り替えできるように
 *
 * @param labelResId ボタンの文字列リソース
 */
private enum class EncodeBottomSheetPage(val labelResId: Int) {
    /** おまかせ設定 */
    Basic(R.string.video_edit_bottomsheet_encode_page_auto),

    /** フレームレートとかを手動で設定したい場合 */
    Advanced(R.string.video_edit_bottomsheet_encode_page_manually)
}

/**
 * 動画の保存画面。エンコード画面
 * TODO バリデーションやってあげたほうが親切かも（コーデックとコンテナ対応しているかとか）
 *
 * @param videoSize 動画の縦横サイズ
 * @param isEnableTenBitHdr 10-bit HDR が有効の場合は true
 * @param onEncode エンコードを押した時に呼ばれる。ファイル名とエンコーダーに渡す設定
 */
@Composable
fun EncodeBottomSheet(
    videoSize: RenderData.Size,
    isEnableTenBitHdr: Boolean,
    onEncode: (String, EncoderParameters) -> Unit
) {
    val currentPage = remember { mutableStateOf(EncodeBottomSheetPage.Basic) }

    // とりあえず高画質で
    val encoderParameters = remember(isEnableTenBitHdr) {
        mutableStateOf(if (isEnableTenBitHdr) EncoderParameters.TEN_BIT_HDR_HIGH_QUALITY else EncoderParameters.HIGH_QUALITY)
    }
    val fileName = remember { mutableStateOf("あかりどろいど_${System.currentTimeMillis()}") }

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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Column {
                Text(
                    text = stringResource(id = R.string.video_edit_bottomsheet_encode_title),
                    fontSize = 24.sp
                )
            }

            // ファイル名
            FileNameInput(
                fileName = fileName.value,
                onFileNameChange = { fileName.value = it },
                extension = encoderParameters.value.containerFormat.extension
            )

            // おまかせ or 手動で設定
            EncodeBottomSheetPageSegmentedButton(
                modifier = Modifier.fillMaxWidth(),
                currentPage = currentPage.value,
                onClick = { currentPage.value = it }
            )

            when (currentPage.value) {
                EncodeBottomSheetPage.Basic -> BasicScreen(
                    isEnableTenBitHdr = isEnableTenBitHdr,
                    encoderParameters = encoderParameters.value,
                    onUpdate = { encoderParameters.value = it }
                )

                EncodeBottomSheetPage.Advanced -> AdvancedScreen(
                    videoSize = videoSize,
                    isEnableTenBitHdr = isEnableTenBitHdr,
                    encoderParameters = encoderParameters.value,
                    onUpdate = { encoderParameters.value = it }
                )
            }

            // ないとスクロールがぎこちない
            Spacer(modifier = Modifier.height(50.dp))
        }

        HorizontalDivider()
        Row(modifier = Modifier.padding(10.dp)) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.video_edit_bottomsheet_encode_long_time_message)
            )
            Button(
                onClick = {
                    onEncode(fileName.value, encoderParameters.value)
                }
            ) {
                Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_start_encode))
            }
        }
    }
}

/**
 * ファイル名入力コンポーネント
 *
 * @param fileName ファイル名
 * @param onFileNameChange ファイル名変更時に呼ばれる
 * @param extension 動画ファイルの拡張子
 */
@Composable
private fun FileNameInput(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    extension: String
) {
    // 保存先
    // RELATIVE_PATH が Android 10 以降
    val savePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${Environment.DIRECTORY_MOVIES}/AkariDroid/${fileName}.$extension"
    } else {
        "${Environment.DIRECTORY_MOVIES}/${fileName}.$extension"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_file_name)) },
            suffix = { Text(text = ".$extension") },
            value = fileName,
            onValueChange = onFileNameChange
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_video_file_24),
                contentDescription = null
            )
            Text(
                text = """
                ${stringResource(id = R.string.video_edit_bottomsheet_encode_file_path_message)}
                $savePath
            """.trimIndent()
            )
        }
    }
}

/**
 * 手動で設定する画面
 *
 * @param videoSize 動画の縦横
 * @param isEnableTenBitHdr 10-bit HDR が有効の場合は true
 * @param encoderParameters [EncoderParameters.AudioVideo]
 * @param onUpdate 更新時に呼ばれる
 */
@Composable
private fun AdvancedScreen(
    videoSize: RenderData.Size,
    isEnableTenBitHdr: Boolean,
    encoderParameters: EncoderParameters.AudioVideo,
    onUpdate: (EncoderParameters.AudioVideo) -> Unit
) {

    fun update(copy: (EncoderParameters.AudioVideo) -> EncoderParameters.AudioVideo) {
        onUpdate(copy(encoderParameters))
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

        // 映像コーデック設定
        VideoEncoderSetting(
            videoSize = videoSize,
            isEnableTenBitHdr = isEnableTenBitHdr,
            videoEncoderParameters = encoderParameters.videoEncoderParameters,
            onUpdate = { videoEncoderParameters -> update { it.copy(videoEncoderParameters = videoEncoderParameters) } }
        )

        // 音声コーデック設定
        AudioEncoderSetting(
            audioEncoderParameters = encoderParameters.audioEncoderParameters,
            onUpdate = { audioEncoderParameters -> update { it.copy(audioEncoderParameters = audioEncoderParameters) } }
        )

        // コンテナフォーマット設定
        ContainerFormatSetting(
            containerFormat = encoderParameters.containerFormat,
            onUpdate = { containerFormat -> update { it.copy(containerFormat = containerFormat) } }
        )

        // エンコード設定を見れるように
        EncoderParametersLog(
            modifier = Modifier.padding(top = 20.dp),
            encoderParameters = encoderParameters
        )
    }
}

/**
 * おまかせ設定
 *
 * @param isEnableTenBitHdr 10-bit HDR が有効の場合は true
 * @param encoderParameters [EncoderParameters.AudioVideo]
 * @param onUpdate 更新時に呼ばれる
 */
@Composable
private fun BasicScreen(
    isEnableTenBitHdr: Boolean,
    encoderParameters: EncoderParameters.AudioVideo,
    onUpdate: (EncoderParameters.AudioVideo) -> Unit
) {
    val currentMenu = remember(encoderParameters, isEnableTenBitHdr) {
        getParametersPresetList(isEnableTenBitHdr).firstOrNull { it.first == encoderParameters }?.second
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

        // 選択肢
        NoOpenableExtendMenu(
            label = stringResource(id = R.string.video_edit_bottomsheet_encode_basic_title),
            iconResId = R.drawable.ic_outline_video_file_24,
            currentMenu = currentMenu?.let { stringResource(id = it) }
        ) {
            getParametersPresetList(isEnableTenBitHdr).forEachIndexed { index, (parameter, titleResId, descriptionResId) ->
                if (index != 0) {
                    HorizontalDivider()
                }
                ExtendMenuItem(
                    title = stringResource(id = titleResId),
                    description = stringResource(id = descriptionResId),
                    isSelect = parameter == encoderParameters,
                    onClick = { onUpdate(parameter) }
                )
            }
        }

        // おまかせ設定があるよカード
        MessageCard(message = stringResource(id = R.string.video_edit_bottomsheet_encode_basic_description))

        // 10-bit HDR 動画だけど、無理やり SDR にしたい場合はプロジェクトの設定を開いてね
        if (isEnableTenBitHdr) {
            MessageCard(message = stringResource(id = R.string.video_edit_bottomsheet_encode_hdr_to_sdr_message))
        }
    }
}

/**
 * おまかせ設定・手動で設定を選ぶボタン
 *
 * @param modifier [Modifier]
 * @param currentPage 開いているページ [EncodeBottomSheetPage]
 * @param onClick 押した時
 */
@Composable
private fun EncodeBottomSheetPageSegmentedButton(
    modifier: Modifier = Modifier,
    currentPage: EncodeBottomSheetPage,
    onClick: (EncodeBottomSheetPage) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        EncodeBottomSheetPage.entries.forEachIndexed { index, page ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = EncodeBottomSheetPage.entries.size),
                onClick = { onClick(page) },
                selected = currentPage == page
            ) {
                Text(text = stringResource(id = page.labelResId))
            }
        }
    }
}

/**
 * エンコード設定を文字列で見れるように
 *
 * @param modifier [Modifier]
 * @param encoderParameters エンコーダーに渡すパラメーター
 */
@Composable
private fun EncoderParametersLog(
    modifier: Modifier,
    encoderParameters: EncoderParameters
) {
    val context = LocalContext.current
    val toString = encoderParameters.toString()

    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_parameter_log_title))

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
                Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_parameter_log_share))
            }
        }
    }
}

/**
 * コンテナフォーマットの設定
 *
 * @param containerFormat [EncoderParameters.ContainerFormat]
 * @param onUpdate 更新時に呼ばれる
 */
@Composable
private fun ContainerFormatSetting(
    containerFormat: EncoderParameters.ContainerFormat,
    onUpdate: (EncoderParameters.ContainerFormat) -> Unit
) {
    val isOpen = remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(id = R.string.video_edit_bottomsheet_encode_container_format_title),
            fontSize = 20.sp
        )

        MessageCard(
            modifier = Modifier.fillMaxWidth(),
            message = stringResource(id = R.string.video_edit_bottomsheet_encode_container_format_description)
        )

        // コンテナフォーマット
        ExtendMenu(
            isOpen = isOpen.value,
            label = stringResource(id = R.string.video_edit_bottomsheet_encode_container_format_title),
            iconResId = R.drawable.ic_outline_video_file_24,
            currentMenu = containerFormat.extension,
            onOpenChange = { isOpen.value = !isOpen.value }
        ) {
            ContainerFormatMenu.forEachIndexed { index, (format, titleResId, descriptionResId) ->
                if (index != 0) {
                    HorizontalDivider()
                }
                ExtendMenuItem(
                    title = stringResource(id = titleResId),
                    description = stringResource(id = descriptionResId),
                    isSelect = format == containerFormat,
                    onClick = { onUpdate(format) }
                )
            }
        }
    }
}

/**
 * 音声エンコーダーの設定
 *
 * @param audioEncoderParameters [EncoderParameters.AudioEncoderParameters]
 * @param onUpdate 更新時に呼ばれる
 */
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
            text = stringResource(id = R.string.video_edit_bottomsheet_encode_audio_encoder_title),
            fontSize = 20.sp
        )

        // 音声コーデック
        ExtendMenu(
            isOpen = isOpen.value,
            label = stringResource(id = R.string.video_edit_bottomsheet_encode_audio_encoder_audio_codec),
            iconResId = R.drawable.ic_outline_audiotrack_24,
            currentMenu = audioEncoderParameters.codec.name,
            onOpenChange = { isOpen.value = !isOpen.value }
        ) {
            AudioCodecMenu.forEachIndexed { index, (codec, titleResId, descriptionResId) ->
                if (index != 0) {
                    HorizontalDivider()
                }
                ExtendMenuItem(
                    title = stringResource(id = titleResId),
                    description = stringResource(id = descriptionResId),
                    isSelect = codec == audioEncoderParameters.codec,
                    onClick = { update { it.copy(codec = codec) } }
                )
            }
        }

        // サンプリングレートの変更はできない
        AudioEncoderSamplingRate()

        // ビットレート
        OutlinedIntTextField(
            modifier = Modifier.fillMaxWidth(),
            value = audioEncoderParameters.bitrate,
            onValueChange = { bitrate -> update { it.copy(bitrate = bitrate) } },
            label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_audio_encoder_audio_bitrate)) },
            suffix = { Text(text = "(${NumberFormat.formatByteUnit(audioEncoderParameters.bitrate)})") }
        )
    }
}

/**
 * 映像エンコーダーの設定
 *
 * @param isEnableTenBitHdr 10-bit HDR をエンコードする場合。今のところ HEVC と AV1 のみ動作確認済みです。
 * @param videoSize 動画の縦横サイズ
 * @param videoEncoderParameters [EncoderParameters.VideoEncoderParameters]
 * @param onUpdate 更新時に呼ばれる
 */
@Composable
private fun VideoEncoderSetting(
    isEnableTenBitHdr: Boolean,
    videoSize: RenderData.Size,
    videoEncoderParameters: EncoderParameters.VideoEncoderParameters,
    onUpdate: (EncoderParameters.VideoEncoderParameters) -> Unit
) {
    val isOpen = remember { mutableStateOf(false) }

    fun update(copy: (EncoderParameters.VideoEncoderParameters) -> EncoderParameters.VideoEncoderParameters) {
        onUpdate(copy(videoEncoderParameters))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Text(
            text = stringResource(id = R.string.video_edit_bottomsheet_encode_video_encoder_title),
            fontSize = 20.sp
        )

        // 動画の縦横はこの画面では変更できないよ
        VideoEncoderVideoWidthHeight(videoSize = videoSize)

        // コーデック
        ExtendMenu(
            isOpen = isOpen.value,
            label = stringResource(id = R.string.video_edit_bottomsheet_encode_video_encoder_video_codec),
            iconResId = R.drawable.ic_outline_video_file_24,
            currentMenu = videoEncoderParameters.codec.name,
            onOpenChange = { isOpen.value = !isOpen.value }
        ) {
            getVideoCodecMenu(isTenBitHdr = isEnableTenBitHdr).forEachIndexed { index, (codec, titleResId, descriptionResId) ->
                if (index != 0) {
                    HorizontalDivider()
                }
                ExtendMenuItem(
                    title = stringResource(id = titleResId),
                    description = stringResource(id = descriptionResId),
                    isSelect = codec == videoEncoderParameters.codec,
                    onClick = { update { it.copy(codec = codec) } }
                )
            }
        }

        // 10-bit HDR 動画のエンコードの場合は HEVC / AV1 しか選べない
        // また、10-bit HDR 動画だけど、無理やり SDR にしたい場合はプロジェクトの設定を開いてね
        if (isEnableTenBitHdr) {
            MessageCard(message = stringResource(id = R.string.video_edit_bottomsheet_encode_video_encoder_ten_bit_hdr_message))
            MessageCard(message = stringResource(id = R.string.video_edit_bottomsheet_encode_hdr_to_sdr_message))
        }

        // ビットレート
        OutlinedIntTextField(
            modifier = Modifier.fillMaxWidth(),
            value = videoEncoderParameters.bitrate,
            onValueChange = { bitrate -> update { it.copy(bitrate = bitrate) } },
            label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_video_encoder_video_bitrate)) },
            suffix = { Text(text = "(${NumberFormat.formatByteUnit(videoEncoderParameters.bitrate)})") }
        )

        // フレームレート
        OutlinedIntTextField(
            modifier = Modifier.fillMaxWidth(),
            value = videoEncoderParameters.frameRate,
            onValueChange = { frameRate -> update { it.copy(frameRate = frameRate) } },
            label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_video_encoder_framerate)) }
        )

        // キーフレーム間隔
        OutlinedIntTextField(
            modifier = Modifier.fillMaxWidth(),
            value = videoEncoderParameters.keyframeInterval,
            onValueChange = { keyframeInterval -> update { it.copy(keyframeInterval = keyframeInterval) } },
            label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_video_encoder_keyframe_interval)) }
        )

    }
}

/**
 * 動画の縦横サイズ
 *
 * @param videoSize 縦横サイズ
 */
@Composable
private fun VideoEncoderVideoWidthHeight(videoSize: RenderData.Size) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.video_edit_bottomsheet_encode_video_encoder_height_width_title),
                fontSize = 18.sp
            )
            Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_video_encoder_height_width_description))
        }

        Text(
            text = "${videoSize.width}x${videoSize.height}",
            fontSize = 20.sp
        )
    }
}

/** 音声のサンプリングレートは変更できないよ。 */
@Composable
private fun AudioEncoderSamplingRate() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.video_edit_bottomsheet_encode_audio_encoder_audio_saplingrate_title),
                fontSize = 18.sp
            )
            Text(text = stringResource(id = R.string.video_edit_bottomsheet_encode_audio_encoder_audio_saplingrate_description))
        }

        Text(
            text = "${AkariCoreAudioProperties.SAMPLING_RATE} (48kHz)",
            fontSize = 20.sp
        )
    }
}

@Composable
private fun HdrOrSdrSetting(
    modifier: Modifier = Modifier,
    isEnableTenBitHdr: Boolean,
    onChange: (isEnableTenBitHdr: Boolean) -> Unit
) {
    val isOpen = remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "HDR / SDR 変換",
            fontSize = 18.sp
        )
        Text(text = "よくわからない場合は HDR のままエンコードしてください。色の変化を許容してまで SDR でエンコードする必要がある場合に利用してください。")
    }

    ExtendMenu(
        isOpen = isOpen.value,
        label = stringResource(id = R.string.video_edit_bottomsheet_encode_video_encoder_video_codec),
        iconResId = R.drawable.android_hdr_icon,
        currentMenu = "10 ビット HDR を維持する",
        onOpenChange = { isOpen.value = !isOpen.value }
    ) {
        listOf("10 ビット HDR を維持する", "SDR に変換する（おすすめしません）").forEach { menu ->
            ExtendMenuItem(
                isSelect = false,
                title = menu,
                description = "",
                onClick = {}
            )
        }
    }

}