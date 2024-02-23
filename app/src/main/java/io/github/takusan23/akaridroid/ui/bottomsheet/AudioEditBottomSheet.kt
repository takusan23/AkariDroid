package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.OutlinedFloatTextField
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent

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
        modifier = Modifier
            .padding(10.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = "音声の編集",
            onComplete = { onUpdate(audioItem.value) },
            onDelete = { onDelete(audioItem.value) }
        )

        OutlinedFloatTextField(
            modifier = Modifier.fillMaxWidth(),
            value = audioItem.value.volume,
            onValueChange = { volume -> update { it.copy(volume = volume) } },
            label = { Text(text = "音量。0から1まで") }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = audioItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )
    }
}