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
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.data.CanvasElementType

/**
 * タイムライン
 *
 * @param modifier [Modifier]
 * @param elementList タイムラインの要素
 * @param onElementClick 要素を押したら呼ばれる
 */
@Composable
fun Timeline(
    modifier: Modifier = Modifier,
    elementList: List<CanvasElementData>,
    onElementClick: (CanvasElementData) -> Unit
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
                        text = "テキスト : ${elementType.text}",
                        onClick = { onElementClick(element) }
                    )
                }
            }
        }
        item {
            TimelineElement(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                text = "動画",
                onClick = {  /* TODO */ }
            )
        }
    }
}