package io.github.takusan23.akaridroid.v2.ui.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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

/**
 * 動画編集画面のボトムバー
 *
 * @param modifier [Modifier]
 * @param onCreateRenderItem [RenderData]を作成したら呼ばれます。動画以外は配列に一つだけ、動画の場合は音声と映像で2つ配列に入っています。
 */
@Composable
fun VideoEditorBottomBar(
    modifier: Modifier = Modifier,
    onCreateRenderItem: (List<RenderData.RenderItem>) -> Unit
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
            AddTextButton(onCreateRenderItem = { onCreateRenderItem(listOf(it)) })
            AddImageButton(onCreateRenderItem = { onCreateRenderItem(listOf(it)) })
            AddVideoButton(onCreateRenderItem = { tracks -> onCreateRenderItem(tracks) })
            AddAudioButton(onCreateRenderItem = { onCreateRenderItem(listOf(it)) })
        }
    }
}

/** テキストを追加ボタン */
@Composable
private fun AddTextButton(onCreateRenderItem: (RenderData.RenderItem) -> Unit) {
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
}

/** 画像を追加ボタン */
@Composable
private fun AddImageButton(onCreateRenderItem: (RenderData.RenderItem) -> Unit) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            onCreateRenderItem(
                RenderData.CanvasItem.Image(
                    filePath = RenderData.FilePath.Uri(uri.toString()),
                    displayTime = RenderData.DisplayTime(0, 1000),
                    position = RenderData.Position(0f, 0f),
                )
            )
        }
    )

    VideoEditorBottomBarItem(
        label = "画像の追加",
        iconId = R.drawable.ic_outline_add_photo_alternate_24px,
        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    )
}

/** 動画を追加ボタン */
@Composable
private fun AddVideoButton(onCreateRenderItem: (List<RenderData.RenderItem>) -> Unit) {
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            // 動画の場合、映像トラックと音声トラックの2つがあるので2つ配列に入れて返す
            // TODO 音声トラックない場合
            onCreateRenderItem(
                listOf(
                    RenderData.CanvasItem.Video(
                        filePath = RenderData.FilePath.Uri(uri.toString()),
                        displayTime = RenderData.DisplayTime(0, 1000),
                        position = RenderData.Position(0f, 0f),
                    ),
                    RenderData.AudioItem.Audio(
                        id = System.currentTimeMillis() + 10,
                        filePath = RenderData.FilePath.Uri(uri.toString()),
                        displayTime = RenderData.DisplayTime(0, 1000)
                    )
                )
            )
        }
    )

    VideoEditorBottomBarItem(
        label = "動画の追加",
        iconId = R.drawable.ic_outline_video_file_24,
        onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
    )
}

/** 音声を追加ボタン */
@Composable
private fun AddAudioButton(onCreateRenderItem: (RenderData.RenderItem) -> Unit) {
    VideoEditorBottomBarItem(
        label = "音声の追加",
        iconId = R.drawable.ic_outline_audiotrack_24,
        onClick = { }
    )
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
