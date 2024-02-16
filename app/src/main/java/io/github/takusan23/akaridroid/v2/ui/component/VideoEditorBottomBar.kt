package io.github.takusan23.akaridroid.v2.ui.component

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.v2.RenderData

/** 動画編集画面のボトムバー */
@Composable
fun VideoEditorBottomBar(
    modifier: Modifier = Modifier,
    onCreateRenderItem: (RenderData.RenderItem) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            VideoEditorBottomBarItem(
                label = "テキストの追加",
                iconId = R.drawable.ic_outline_text_fields_24,
                onClick = {
                    onCreateRenderItem(
                        RenderData.CanvasItem.Text(
                            text = "",
                            displayTime = RenderData.DisplayTime(0, 1000),
                            position = RenderData.Position(0f, 0f)
                        )
                    )
                }
            )
            VideoEditorBottomBarItem(
                label = "動画の追加",
                iconId = R.drawable.ic_outline_video_file_24,
                onClick = { }
            )
            VideoEditorBottomBarItem(
                label = "音声の追加",
                iconId = R.drawable.ic_outline_audiotrack_24,
                onClick = { }
            )
        }
    }
}

/** [VideoEditorBottomBar]の各アイテム */
@Composable
private fun VideoEditorBottomBarItem(
    modifier: Modifier = Modifier,
    label: String,
    iconId: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(painter = painterResource(id = iconId), contentDescription = null)
            Text(text = label)
        }
    }
}
