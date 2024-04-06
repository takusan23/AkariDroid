package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemFloatEditComponent

/**
 * [RenderData.AudioItem.Audio]の編集ボトムシート
 *
 * @param renderItem 音声素材の情報
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun AudioEditBottomSheet(
    renderItem: RenderData.AudioItem.Audio,
    onUpdate: (RenderData.AudioItem.Audio) -> Unit,
    onDelete: (RenderData.AudioItem.Audio) -> Unit,
) {
    val audioItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.AudioItem.Audio) -> RenderData.AudioItem.Audio) {
        audioItem.value = copy(audioItem.value)
    }

    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_audio_title),
            onComplete = { onUpdate(audioItem.value) },
            onDelete = { onDelete(audioItem.value) }
        )

        RenderItemFloatEditComponent(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.video_edit_bottomsheet_audio_volume),
            iconResId = R.drawable.ic_outlined_volume_up_24px,
            value = audioItem.value.volume,
            onChange = { volume -> update { it.copy(volume = volume) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = audioItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )
    }
}