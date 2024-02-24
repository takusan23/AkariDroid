package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.OutlinedIntTextField
import io.github.takusan23.akaridroid.ui.component.OutlinedLongTextField
import java.text.SimpleDateFormat
import java.util.Locale

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
    val simpleDateFormat = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }
    val renderData = remember { mutableStateOf(renderData) }

    fun update(copy: (RenderData) -> RenderData) {
        renderData.value = copy(renderData.value)
    }

    Column(
        modifier = Modifier
            .padding(10.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = "動画情報の編集",
            onComplete = { onUpdate(renderData.value) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedLongTextField(
                modifier = Modifier.weight(1f),
                value = renderData.value.durationMs,
                onValueChange = { long -> update { it.copy(durationMs = long) } },
                label = { Text(text = "動画の時間 (ミリ秒)") }
            )
            Text(
                modifier = Modifier.padding(vertical = 10.dp),
                text = simpleDateFormat.format(renderData.value.durationMs)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            OutlinedIntTextField(
                modifier = Modifier.weight(1f),
                value = renderData.value.videoSize.width,
                onValueChange = { int -> update { it.copy(videoSize = it.videoSize.copy(width = int)) } },
                label = { Text(text = "動画の幅") }
            )
            OutlinedIntTextField(
                modifier = Modifier.weight(1f),
                value = renderData.value.videoSize.height,
                onValueChange = { int -> update { it.copy(videoSize = it.videoSize.copy(height = int)) } },
                label = { Text(text = "動画の高さ") }
            )
        }
    }
}