package io.github.takusan23.akaridroid.v2.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.v2.viewmodel.VideoEditorViewModel

/** 動画編集画面 */
@Composable
fun VideoEditorScreenV2(viewModel: VideoEditorViewModel = viewModel()) {

    /** 動画の素材や情報が入ったデータ */
    val renderData = viewModel.renderData.collectAsState()

    /** プレビューのBitmap */
    val previewBitmap = viewModel.videoEditorPreviewPlayer.previewBitmap.collectAsState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // プレビュー
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .border(1.dp, MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap.value != null) {
                    Image(
                        modifier = Modifier.matchParentSize(),
                        bitmap = previewBitmap.value!!.asImageBitmap(),
                        contentDescription = null
                    )
                } else {
                    Text(text = "生成中です...")
                }

            }

            renderData.value.canvasRenderItem.forEach {
                Text(text = it.toString())
                Divider()
            }
        }
    }
}