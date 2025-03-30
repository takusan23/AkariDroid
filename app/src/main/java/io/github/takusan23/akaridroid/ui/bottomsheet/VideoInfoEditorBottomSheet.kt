package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.DurationInput
import io.github.takusan23.akaridroid.ui.component.OutlinedIntTextField

/**
 * 動画情報編集ボトムシート。
 * 動画時間とか、動画の縦横サイズとか。
 *
 * @param renderData 動画情報
 * @param onUpdate 更新時に呼ばれる
 */
@Composable
fun VideoInfoEditorBottomSheet(
    renderData: RenderData,
    onUpdate: (RenderData) -> Unit
) {
    val renderData = remember { mutableStateOf(renderData) }

    fun update(copy: (RenderData) -> RenderData) {
        renderData.value = copy(renderData.value)
    }

    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        BottomSheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_title),
            onComplete = { onUpdate(renderData.value) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_video_duration))
            DurationInput(
                modifier = Modifier.weight(1f),
                durationMs = renderData.value.durationMs,
                onChange = { long -> update { it.copy(durationMs = long) } }
            )
        }

        // TODO 動画解像度の編集、選択肢から選べるようにしたい。打ち込むのめんどい
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            OutlinedIntTextField(
                modifier = Modifier.weight(1f),
                value = renderData.value.videoSize.width,
                onValueChange = { int -> update { it.copy(videoSize = it.videoSize.copy(width = int)) } },
                label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_video_width)) }
            )
            OutlinedIntTextField(
                modifier = Modifier.weight(1f),
                value = renderData.value.videoSize.height,
                onValueChange = { int -> update { it.copy(videoSize = it.videoSize.copy(height = int)) } },
                label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_videoinfo_video_height)) }
            )
        }

        TenBitHdrSettingItem(
            modifier = Modifier.fillMaxWidth(),
            currentColorSpace = renderData.value.colorSpace,
            onChange = { colorSpace -> update { it.copy(colorSpace = colorSpace) } }
        )
    }
}