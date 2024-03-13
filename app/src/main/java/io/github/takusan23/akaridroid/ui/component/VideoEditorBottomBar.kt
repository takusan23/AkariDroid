package io.github.takusan23.akaridroid.ui.component

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/** 何を追加したか */
sealed interface VideoEditorBottomBarAddItem {
    /** テキスト */
    data object Text : VideoEditorBottomBarAddItem

    /** 図形 */
    data object Shape : VideoEditorBottomBarAddItem

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
 * @param onVideoInfoClick 仮だけど動画情報編集画面を開くボタン
 * @param onSettingClick 設定画面を開く（仮）
 * @param onTimeLineReset タイムラインのリセット
 */
@Composable
fun VideoEditorBottomBar(
    modifier: Modifier = Modifier,
    onCreateRenderItem: (VideoEditorBottomBarAddItem) -> Unit,
    onEncodeClick: () -> Unit,
    onVideoInfoClick: () -> Unit,
    onSettingClick: () -> Unit,
    onTimeLineReset: () -> Unit
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
            AddShapeButton(onCreateRenderItem)

            // 動画情報編集画面
            VideoEditorBottomBarItem(
                label = "動画情報の編集",
                iconId = R.drawable.ic_outline_movie_edit_24,
                onClick = onVideoInfoClick
            )

            // タイムラインのリセット。全て破棄する
            TimeLineResetButton(onReset = onTimeLineReset)

            // エンコードボタン、仮
            VideoEditorBottomBarItem(
                label = "エンコードする",
                iconId = R.drawable.ic_outline_save_24,
                onClick = onEncodeClick
            )

            VideoEditorBottomBarItem(
                label = "設定",
                iconId = R.drawable.ic_outline_settings_24px,
                onClick = onSettingClick
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

/** 図形を追加する */
@Composable
private fun AddShapeButton(onCreateRenderItem: (VideoEditorBottomBarAddItem) -> Unit) {
    VideoEditorBottomBarItem(
        label = "図形の追加",
        iconId = R.drawable.ic_outline_category_24,
        onClick = { onCreateRenderItem(VideoEditorBottomBarAddItem.Shape) }
    )
}

/**
 * タイムラインのアイテムをすべてリセットする。
 * @param onReset ダイアログが出て、本当に削除する場合に呼ばれる
 */
@Composable
private fun TimeLineResetButton(onReset: () -> Unit) {
    val isVisibleDialog = remember { mutableStateOf(false) }

    if (isVisibleDialog.value) {
        AlertDialog(
            onDismissRequest = { isVisibleDialog.value = false },
            icon = { Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null) },
            title = { Text(text = "本当に破棄しますか") },
            text = { Text(text = "タイムラインの素材を全て破棄して、まっさらな状態にします。本当に破棄しますか？") },
            dismissButton = {
                TextButton(onClick = { isVisibleDialog.value = false }) {
                    Text(text = "戻る")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReset()
                        isVisibleDialog.value = false
                    }
                ) {
                    Text(text = "破棄する")
                }
            }
        )
    }

    VideoEditorBottomBarItem(
        label = "タイムラインを破棄",
        iconId = R.drawable.ic_outline_delete_24px,
        onClick = { isVisibleDialog.value = true }
    )
}