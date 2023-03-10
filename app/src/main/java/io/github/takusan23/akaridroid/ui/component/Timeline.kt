package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.data.AudioAssetData
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.data.CanvasElementType
import io.github.takusan23.akaridroid.data.VideoFileData

/**
 * タイムライン
 *
 * @param modifier [Modifier]
 * @param elementList タイムラインの要素
 * @param videoFileData 動画データ
 * @param audioAssetList 音声素材リスト
 * @param onElementClick 要素を押したら呼ばれる
 */
@Composable
fun Timeline(
    modifier: Modifier = Modifier,
    videoFileData: VideoFileData?,
    elementList: List<CanvasElementData>,
    audioAssetList: List<AudioAssetData>,
    onElementClick: (CanvasElementData) -> Unit,
    onAudioAssetClick: (AudioAssetData) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(elementList) { element ->
            when (val elementType = element.elementType) {
                is CanvasElementType.TextElement -> {
                    TimelineElement(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        type = TimelineElementType.Text,
                        text = elementType.text,
                        onClick = { onElementClick(element) }
                    )
                }
            }
        }
        if (videoFileData != null) {
            item {
                TimelineElement(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    type = TimelineElementType.Video,
                    text = videoFileData.fileName,
                    onClick = {  /* TODO */ }
                )
            }
        }
        items(audioAssetList) { audioAssetData ->
            TimelineElement(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                type = TimelineElementType.Audio,
                text = audioAssetData.fileName,
                onClick = { onAudioAssetClick(audioAssetData) }
            )
        }
    }
}