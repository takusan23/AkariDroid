package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer
import io.github.takusan23.akaridroid.ui.component.data.TouchEditorData

/**
 * プレビュー部分のコンテナ
 * タッチ編集機能と、プレビュー再生機能の切り替えがある
 *
 * @param modifier [Modifier]
 * @param previewBitmap プレビューBitmap。[io.github.takusan23.akaridroid.preview.VideoEditorPreviewPlayer.previewBitmap]
 * @param touchEditorData 現在表示されているキャンバス要素を[TouchEditorData]で
 * @param onDragAndDropEnd タッチ操作で移動が終わったら呼ばれる。[TouchEditorData.PositionUpdateRequest]
 * @param onSizeChangeRequest ピンチイン、ピンチアウトでサイズ変更されたら呼ばれる。[TouchEditorData.SizeChangeRequest]
 * @param playerStatus 再生状態。再生位置とか再生中かとか
 * @param onPlayOrPause 再生か一時停止を押した時
 * @param onSeek シークしたとき
 */
@Composable
fun PreviewContainer(
    modifier: Modifier = Modifier,
    previewBitmap: ImageBitmap?,
    touchEditorData: TouchEditorData,
    onDragAndDropEnd: (TouchEditorData.PositionUpdateRequest) -> Unit,
    onSizeChangeRequest: (TouchEditorData.SizeChangeRequest) -> Unit,
    playerStatus: VideoEditorPreviewPlayer.PlayerStatus,
    onPlayOrPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    // タッチ移動機能か、プレビュー再生か
    val currentMode = remember { mutableStateOf(PreviewOrTouchEditMode.TouchEdit) }

    Box(modifier = modifier) {

        // プレビューを出す
        if (previewBitmap != null) {
            Image(
                modifier = Modifier
                    .matchParentSize()
                    .align(Alignment.Center),
                bitmap = previewBitmap,
                contentDescription = null
            )
        } else {
            Text(text = stringResource(id = R.string.video_preview_generating))
        }

        // プレビュー再生のコントローラーか、タッチ編集モードか
        when (currentMode.value) {
            PreviewOrTouchEditMode.Preview -> PreviewControlPanel(
                modifier = Modifier.matchParentSize(),
                playerStatus = playerStatus,
                onPlayOrPause = onPlayOrPause,
                onSeek = onSeek
            )

            PreviewOrTouchEditMode.TouchEdit -> TouchEditor(
                modifier = Modifier
                    .matchParentSize()
                    .align(Alignment.Center),
                videoSize = touchEditorData.videoSize,
                touchEditorItemList = touchEditorData.visibleTouchEditorItemList,
                onDragAndDropEnd = onDragAndDropEnd,
                onSizeChangeRequest = onSizeChangeRequest
            )
        }

        // タッチ移動・プレビュー再生の切り替えモードスイッチ
        PreviewOrTouchEditSegmentButton(
            modifier = Modifier
                .padding(5.dp)
                .align(Alignment.TopEnd),
            currentMode = currentMode.value,
            onPreviewClick = { currentMode.value = PreviewOrTouchEditMode.Preview },
            onTouchEditClick = {
                currentMode.value = PreviewOrTouchEditMode.TouchEdit
                // タッチ編集モードにしたら再生も止める
                if (playerStatus.isPlaying) {
                    onPlayOrPause()
                }
            }
        )

        // おしらせ（プレビューがとても遅いけど、出力には関係ないよ）
        PreviewNoticeDialogButton(modifier = Modifier.align(Alignment.TopStart))
    }
}
