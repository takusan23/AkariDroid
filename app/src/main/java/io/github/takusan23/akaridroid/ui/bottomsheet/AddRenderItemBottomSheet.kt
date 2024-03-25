package io.github.takusan23.akaridroid.ui.bottomsheet

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.ui.component.BottomSheetMenuItem

/**
 * タイムラインに素材を追加するボトムシート
 *
 * @param onAddRenderItem 何を追加したか[AddRenderItem]
 * @param onStartAkaLink あかりんく開始を押した時
 */
@Composable
fun AddRenderItemBottomSheet(
    onAddRenderItem: (AddRenderItem) -> Unit,
    onStartAkaLink: () -> Unit
) {
    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "タイムラインへ追加",
            fontSize = 24.sp
        )

        BottomSheetMenuItem(
            title = "テキスト",
            description = "テキストを追加します",
            iconResId = R.drawable.ic_outline_text_fields_24,
            onClick = { onAddRenderItem(AddRenderItem.Text) }
        )
        PhotoPickerBottomSheetMenuItem(
            title = "画像",
            description = "画像を追加します。",
            iconResId = R.drawable.ic_outline_add_photo_alternate_24px,
            isImageOnly = true,
            onResultUri = { uri -> onAddRenderItem(AddRenderItem.Image(uri)) }
        )
        PhotoPickerBottomSheetMenuItem(
            title = "動画",
            description = "動画を追加します。性能が許す限りタイムラインに追加できるはず？",
            iconResId = R.drawable.ic_outlined_movie_24px,
            isImageOnly = false,
            onResultUri = { uri -> onAddRenderItem(AddRenderItem.Video(uri)) }
        )
        FilePickerBottomSheetMenuItem(
            title = "音声",
            description = "音声を追加します。",
            iconResId = R.drawable.ic_outline_audiotrack_24,
            mimeType = "audio/*",
            onResultUri = { uri -> onAddRenderItem(AddRenderItem.Audio(uri)) }
        )
        BottomSheetMenuItem(
            title = "あかりんく（外部連携機能）を始める",
            description = "外部アプリで素材を作成し、タイムラインに追加できる機能です。対応しているアプリが必要です。",
            iconResId = R.drawable.akari_droid_icon,
            onClick = onStartAkaLink
        )
    }
}

/**
 * ファイルピッカーを開く [BottomSheetMenuItem]
 *
 * @param mimeType MIME-Type
 * @param onResultUri 受け取った Uri
 */
@Composable
private fun FilePickerBottomSheetMenuItem(
    title: String,
    description: String,
    iconResId: Int,
    mimeType: String,
    onResultUri: (Uri) -> Unit
) {
    // フォトピッカーの音声版は存在しないので、、、
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            onResultUri(uri)
        }
    )

    BottomSheetMenuItem(
        title = title,
        description = description,
        iconResId = iconResId,
        onClick = { filePicker.launch(arrayOf(mimeType)) }
    )
}

/**
 * フォトピッカーを開く [BottomSheetMenuItem]
 *
 * @param isImageOnly 画像のみは true
 * @param onResultUri 受け取った Uri
 */
@Composable
private fun PhotoPickerBottomSheetMenuItem(
    title: String,
    description: String,
    iconResId: Int,
    isImageOnly: Boolean,
    onResultUri: (Uri) -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            onResultUri(uri)
        }
    )

    BottomSheetMenuItem(
        title = title,
        description = description,
        iconResId = iconResId,
        onClick = { photoPicker.launch(PickVisualMediaRequest(if (isImageOnly) ActivityResultContracts.PickVisualMedia.ImageOnly else ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
    )

}