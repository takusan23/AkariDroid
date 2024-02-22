package io.github.takusan23.akaridroid.v2.ui.component

import android.net.Uri
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

/** 何を追加したか */
sealed interface VideoEditorBottomBarAddItem {
    /** テキスト */
    data object Text : VideoEditorBottomBarAddItem

    /** 画像 */
    data class Image(val uri: Uri) : VideoEditorBottomBarAddItem

    /** 動画 */
    data class Video(val uri: Uri) : VideoEditorBottomBarAddItem

    /** 音声 */
    data class Audio(val uri: Uri) : VideoEditorBottomBarAddItem
}

/**
 * 動画編集画面のボトムバー
 *
 * @param modifier [Modifier]
 * @param onCreateRenderItem 素材を追加すると呼ばれる[VideoEditorBottomBarAddItem]
 * @param onEncodeClick 仮だけどエンコードボタン
 */
@Composable
fun VideoEditorBottomBar(
    modifier: Modifier = Modifier,
    onCreateRenderItem: (VideoEditorBottomBarAddItem) -> Unit,
    onEncodeClick: () -> Unit
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
            AddTextButton(onCreateRenderItem)
            AddImageButton(onCreateRenderItem)
            AddVideoButton(onCreateRenderItem)
            AddAudioButton(onCreateRenderItem)

            // エンコードボタン、仮
            VideoEditorBottomBarItem(
                label = "エンコードする",
                iconId = R.drawable.ic_outline_save_24,
                onClick = onEncodeClick
            )
        }
    }
}

/** テキストを追加ボタン */
@Composable
private fun AddTextButton(onCreateRenderItem: (VideoEditorBottomBarAddItem) -> Unit) {
    VideoEditorBottomBarItem(
        label = "テキストの追加",
        iconId = R.drawable.ic_outline_text_fields_24,
        onClick = {
            onCreateRenderItem(VideoEditorBottomBarAddItem.Text)
        }
    )
}

/** 画像を追加ボタン */
@Composable
private fun AddImageButton(onCreateRenderItem: (VideoEditorBottomBarAddItem) -> Unit) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            onCreateRenderItem(VideoEditorBottomBarAddItem.Image(uri))
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
private fun AddVideoButton(onCreateRenderItem: (VideoEditorBottomBarAddItem) -> Unit) {
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            // 動画の場合、映像トラックと音声トラックの2つがあるので2つ配列に入れて返す
            // TODO 音声トラックない場合
            onCreateRenderItem(VideoEditorBottomBarAddItem.Video(uri))
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
private fun AddAudioButton(onCreateRenderItem: (VideoEditorBottomBarAddItem) -> Unit) {
    // フォトピッカーの音声版は存在しないので、、、
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            onCreateRenderItem(VideoEditorBottomBarAddItem.Audio(uri))
        }
    )

    VideoEditorBottomBarItem(
        label = "音声の追加",
        iconId = R.drawable.ic_outline_audiotrack_24,
        onClick = { filePicker.launch(arrayOf("audio/*")) }
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
