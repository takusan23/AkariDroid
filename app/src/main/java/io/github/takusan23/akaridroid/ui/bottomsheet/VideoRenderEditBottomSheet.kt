package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.canvasrender.itemrender.calcVideoFramePositionMs
import io.github.takusan23.akaridroid.tool.ColorTool
import io.github.takusan23.akaridroid.tool.UriTool
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.ChromaKeyColorDialog
import io.github.takusan23.akaridroid.ui.component.ColorItem
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemPositionEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemRotationEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemSizeEditComponent

/**
 * [RenderData.CanvasItem.Video]の編集ボトムシート
 *
 * @param renderItem 動画素材の情報
 * @param previewPositionMs プレビューの時間
 * @param isProjectHdr プロジェクトで 10-bit HDR が有効の場合
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 * @param onOpenVideoInfo 動画情報編集ボトムシートを開いて欲しいときに呼ばれる
 */
@Composable
fun VideoRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Video,
    previewPositionMs: Long,
    isProjectHdr: Boolean,
    onUpdate: (RenderData.CanvasItem.Video) -> Unit,
    onDelete: (RenderData.CanvasItem.Video) -> Unit,
    onOpenVideoInfo: () -> Unit
) {
    val context = LocalContext.current
    val videoItem = remember { mutableStateOf(renderItem) }
    val videoFileName = remember { mutableStateOf<String?>(null) }

    fun update(copy: (RenderData.CanvasItem.Video) -> RenderData.CanvasItem.Video) {
        videoItem.value = copy(videoItem.value)
    }

    LaunchedEffect(key1 = videoItem.value.filePath) {
        // Uri しか来ないので
        (videoItem.value.filePath as? RenderData.FilePath.Uri)?.also { filePath ->
            videoFileName.value = UriTool.getFileName(context, filePath.uriPath.toUri())
        }
    }

    Column(
        modifier = Modifier.bottomSheetPadding(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_video_title),
            onComplete = { onUpdate(videoItem.value) },
            onDelete = { onDelete(videoItem.value) }
        )

        Text(text = "${stringResource(id = R.string.video_edit_bottomsheet_video_file_name)} : ${videoFileName.value}")

        // 10-bit HDR の動画なのに 10-bit HDR 動画編集機能がオフの場合
        if (renderItem.dynamicRange == RenderData.CanvasItem.Video.DynamicRange.HDR_HLG && !isProjectHdr) {
            ProjectSdrMessageInHdrVideo(
                modifier = Modifier.padding(vertical = 10.dp),
                onOpenVideoInfo = onOpenVideoInfo
            )
        }

        RenderItemChromaKeyEditComponent(
            filePath = videoItem.value.filePath,
            previewPositionMs = videoItem.value.calcVideoFramePositionMs(previewPositionMs),
            chromaKeyColorOrNull = videoItem.value.chromaKeyColor,
            onUpdate = { chromaKeyColor -> update { it.copy(chromaKeyColor = chromaKeyColor) } }
        )

        RenderItemPositionEditComponent(
            position = videoItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            isEditPlayback = true,
            displayTime = videoItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )

        RenderItemSizeEditComponent(
            size = videoItem.value.size,
            onUpdate = { size -> update { it.copy(size = size) } }
        )

        RenderItemRotationEditComponent(
            rotation = videoItem.value.rotation,
            onUpdate = { rotation -> update { it.copy(rotation = rotation) } }
        )
    }
}

/**
 * プロジェクトで 10-bit HDR が無効の場合で、10-bit HDR 動画を編集しているときに表示するメッセージ
 *
 * @param modifier [Modifier]
 * @param onOpenVideoInfo 動画情報編集ボトムシートを開いて欲しいときに呼ばれる
 */
@Composable
private fun ProjectSdrMessageInHdrVideo(
    modifier: Modifier = Modifier,
    onOpenVideoInfo: () -> Unit
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {

            Text(text = stringResource(id = R.string.video_edit_renderitem_warning_disable_ten_bit_hdr_edit_title))

            OutlinedButton(onClick = onOpenVideoInfo) {
                Text(text = stringResource(id = R.string.video_edit_renderitem_warning_disable_ten_bit_hdr_edit_button))
            }
        }
    }
}

/**
 * クロマキー合成の色選択メニューコンポーネント
 *
 * @param modifier [Modifier]
 * @param filePath 動画ファイルのパス。[RenderData.FilePath]参照。
 * @param previewPositionMs プレビューの再生位置
 * @param chromaKeyColorOrNull クロマキーの透過色
 * @param onUpdate 透過色更新時、もしくはクロマキーしない場合は null。
 */
@Composable
private fun RenderItemChromaKeyEditComponent(
    modifier: Modifier = Modifier,
    filePath: RenderData.FilePath,
    previewPositionMs: Long,
    chromaKeyColorOrNull: Int?,
    onUpdate: (Int?) -> Unit
) {
    // Int を Color に
    val colorOrDefault = chromaKeyColorOrNull?.let { Color(it) } ?: Color.White
    // パースできなかったら白
    val isShowChromaKeyDialog = remember { mutableStateOf(false) }
    val isOpenChromaKeyMenu = remember { mutableStateOf(chromaKeyColorOrNull != null) }

    // ダイアログを出す
    if (isShowChromaKeyDialog.value) {
        ChromaKeyColorDialog(
            filePath = filePath,
            chromakeyColor = colorOrDefault,
            previewPositionMs = previewPositionMs,
            onDismissRequest = { isShowChromaKeyDialog.value = false },
            onChange = { color -> onUpdate(color.toArgb()) }
        )
    }

    Column(modifier = modifier.padding(10.dp)) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                painter = painterResource(id = R.drawable.ic_background_replace_24px),
                contentDescription = null
            )

            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.video_edit_renderitem_chromakey_title)
            )

            Switch(
                checked = isOpenChromaKeyMenu.value,
                onCheckedChange = {
                    // false にしたら null に
                    isOpenChromaKeyMenu.value = it
                    onUpdate(if (it) Color.Transparent.toArgb() else null)
                }
            )
        }

        if (isOpenChromaKeyMenu.value) {
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                ColorItem(color = colorOrDefault)

                Text(text = ColorTool.toHexColorCode(colorOrDefault))

                OutlinedButton(onClick = { isShowChromaKeyDialog.value = true }) {
                    Icon(painter = painterResource(id = R.drawable.ic_outline_format_color_fill_24px), contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(id = R.string.video_edit_renderitem_chromakey_select_color))
                }
            }
        }
    }

}