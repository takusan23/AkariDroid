package io.github.takusan23.akaridroid.ui.bottomsheet

import android.media.MediaFormat
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.tool.AvAnalyze
import io.github.takusan23.akaridroid.tool.data.toIoType
import io.github.takusan23.akaridroid.ui.component.ExtendMenu
import io.github.takusan23.akaridroid.ui.component.ExtendMenuItem
import io.github.takusan23.akaridroid.ui.component.MessageCard
import kotlinx.coroutines.launch

/**
 * 10-bit HDR を有効にするスイッチ。
 * 有効にする前にボトムシートが表示される。
 *
 * @param modifier [Modifier]
 * @param currentColorSpace 選択中の色空間（SDR / HDR）
 * @param onChange 変更時
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenBitHdrSettingItem(
    modifier: Modifier = Modifier,
    currentColorSpace: RenderData.ColorSpace,
    onChange: (RenderData.ColorSpace) -> Unit
) {
    val isShowBottomSheet = remember { mutableStateOf(false) }

    TenBitHdrSwitchSettingItem(
        modifier = modifier,
        isEnableTenBitHdr = currentColorSpace.isHdr,
        onTenBitHdrChange = { isShowBottomSheet.value = true },
        // TODO 10-bit HDR 動画編集をサポートしているのが Android 13 以降（Camera2 API がそうだからそのハズ）。本当はエンコーダー、OpenGL ES 共に HDR に対応しているかを見る必要があるがやっていない
        isEnable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    )

    // このボトムシートからしか使わないはずなので、VideoEditorBottomSheetRouteRequestData 一覧にはない
    if (isShowBottomSheet.value) {
        ModalBottomSheet(onDismissRequest = { isShowBottomSheet.value = false }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_title),
                    fontSize = 24.sp
                )

                HorizontalDivider()

                Icon(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    painter = painterResource(R.drawable.android_hdr_description),
                    contentDescription = null
                )

                // SDR / HDR( HLG / PQ ) 選択
                val isOpen = remember { mutableStateOf(true) }
                ExtendMenu(
                    isOpen = isOpen.value,
                    label = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_menu_title),
                    iconResId = R.drawable.android_hdr_icon,
                    currentMenu = currentColorSpace.name,
                    onOpenChange = { isOpen.value = !isOpen.value }
                ) {
                    RenderData.ColorSpace.entries.forEachIndexed { index, colorSpace ->
                        if (index != 0) {
                            HorizontalDivider()
                        }
                        ExtendMenuItem(
                            title = colorSpace.name,
                            description = when (colorSpace) {
                                RenderData.ColorSpace.SDR_BT709 -> stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_menu_sdr)
                                RenderData.ColorSpace.HDR_BT2020_HLG -> stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_menu_hdr_hlg)
                                RenderData.ColorSpace.HDR_BT2020_PQ -> stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_menu_hdr_pq)
                            },
                            isSelect = currentColorSpace == colorSpace,
                            onClick = { onChange(colorSpace) }
                        )
                    }
                }

                // SDR/HDR 解析ボタン
                AnalyzeMessage(onResult = onChange)

                // 長い説明
                Text(text = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_enable_description))
            }
        }
    }
}

/**
 * 設定項目
 *
 * @param modifier [Modifier]
 * @param isEnable HDR 動画編集に明らかに対応していない場合は false に
 * @param isEnableTenBitHdr 10-bit HDR が有効の場合
 * @param onTenBitHdrChange スイッチを押した時
 */
@Composable
private fun TenBitHdrSwitchSettingItem(
    modifier: Modifier = Modifier,
    isEnable: Boolean = true,
    isEnableTenBitHdr: Boolean,
    onTenBitHdrChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.toggleable(
            enabled = isEnable,
            value = isEnableTenBitHdr,
            onValueChange = { onTenBitHdrChange(it) },
            role = Role.Switch
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.android_hdr_icon),
            contentDescription = null
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_title),
                fontSize = 20.sp
            )
            Text(text = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_description))
        }
        Switch(
            checked = isEnableTenBitHdr,
            onCheckedChange = null,
            enabled = isEnable
        )
    }
}

/**
 * 解析ボタンの Card
 *
 * @param modifier [Modifier]
 * @param onResult 解析結果
 */
@Composable
private fun AnalyzeMessage(
    modifier: Modifier = Modifier,
    onResult: (RenderData.ColorSpace) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = {
            it ?: return@rememberLauncherForActivityResult
            scope.launch {
                val result = AvAnalyze.analyzeVideo(context, it.toIoType())
                val hdrInfo = result?.tenBitHdrInfoOrSdrNull
                val colorSpace = when {
                    hdrInfo?.colorStandard == MediaFormat.COLOR_STANDARD_BT2020 && hdrInfo.colorTransfer == MediaFormat.COLOR_TRANSFER_HLG -> RenderData.ColorSpace.HDR_BT2020_HLG
                    hdrInfo?.colorStandard == MediaFormat.COLOR_STANDARD_BT2020 && hdrInfo.colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084 -> RenderData.ColorSpace.HDR_BT2020_PQ
                    else -> RenderData.ColorSpace.SDR_BT709
                }
                Toast.makeText(context, "${context.getString(R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_analyze_complete)}$colorSpace", Toast.LENGTH_SHORT).show()
                onResult(colorSpace)
            }
        }
    )

    MessageCard(
        modifier = modifier,
        message = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_analyze_message),
        buttonText = "動画を選択",
        onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
    )
}