package io.github.takusan23.akaridroid.ui.bottomsheet

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R

/**
 * 10Bit HDR を有効にするスイッチ。
 * 有効にする前にボトムシートが表示される。
 *
 * @param modifier [Modifier]
 * @param isEnableTenBitHdr 10Bit HDR が有効の場合
 * @param onTenBitHdrChange 10Bit HDR の有効無効が切り替わった時
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenBitHdrSettingItem(
    modifier: Modifier = Modifier,
    isEnableTenBitHdr: Boolean,
    onTenBitHdrChange: (Boolean) -> Unit
) {
    val isShowBottomSheet = remember { mutableStateOf(false) }

    TenBitHdrSwitchSettingItem(
        modifier = modifier,
        isEnableTenBitHdr = isEnableTenBitHdr,
        onTenBitHdrChange = { isShowBottomSheet.value = true }
    )

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

                TenBitHdrSwitch(
                    isEnableTenBitHdr = isEnableTenBitHdr,
                    onEnableChange = {
                        onTenBitHdrChange(it)
                        isShowBottomSheet.value = false
                    }
                )

                HorizontalDivider()

                Icon(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    painter = painterResource(R.drawable.android_hdr_description),
                    contentDescription = null
                )

                Text(text = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_enable_description))
            }
        }
    }
}

/**
 * 設定項目
 *
 * @param modifier [Modifier]
 * @param isEnableTenBitHdr 10Bit HDR が有効の場合
 * @param onTenBitHdrChange スイッチを押した時
 */
@Composable
private fun TenBitHdrSwitchSettingItem(
    modifier: Modifier = Modifier,
    isEnableTenBitHdr: Boolean,
    onTenBitHdrChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.toggleable(
            value = isEnableTenBitHdr,
            onValueChange = { onTenBitHdrChange(!isEnableTenBitHdr) },
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
            onCheckedChange = null
        )
    }
}

/**
 * ボトムシート内の 10Bit HDR 切り替えスイッチ
 *
 * @param modifier [Modifier]
 * @param isEnableTenBitHdr 10Bit HDR が有効の場合は true
 * @param onTenBitHdrChange 10Bit HDR の有効無効が切り替わった時
 */
@Composable
private fun TenBitHdrSwitch(
    modifier: Modifier = Modifier,
    isEnableTenBitHdr: Boolean,
    onEnableChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.toggleable(
            value = isEnableTenBitHdr,
            onValueChange = { onEnableChange(!isEnableTenBitHdr) },
            role = Role.Switch
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.android_hdr_icon),
            contentDescription = null
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_ten_bit_hdr_enable_title),
            fontSize = 20.sp
        )
        Switch(
            checked = isEnableTenBitHdr,
            onCheckedChange = null
        )
    }
}