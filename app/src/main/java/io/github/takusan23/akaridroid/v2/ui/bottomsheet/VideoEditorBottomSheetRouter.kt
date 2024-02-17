package io.github.takusan23.akaridroid.v2.ui.bottomsheet

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import io.github.takusan23.akaridroid.v2.RenderData

/**
 * 動画編集画面で使うボトムシートを出す画面
 *
 * @param videoEditorBottomSheetRouteRequestData ボトムシートの表示に必要なデータ
 * @param onResult ボトムシートで作業終わると[VideoEditorBottomSheetRouteResultData]が返される
 * @param onClose ボトムシート閉じたときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorBottomSheetRouter(
    videoEditorBottomSheetRouteRequestData: VideoEditorBottomSheetRouteRequestData,
    onResult: (VideoEditorBottomSheetRouteResultData) -> Unit,
    onClose: () -> Unit
) {

    ModalBottomSheet(
        windowInsets = WindowInsets(0, 0, 0, 0),
        onDismissRequest = onClose
    ) {

        when (videoEditorBottomSheetRouteRequestData) {

            // 編集画面を出す
            is VideoEditorBottomSheetRouteRequestData.OpenEditor -> when (videoEditorBottomSheetRouteRequestData.renderItem) {
                is RenderData.AudioItem.Audio -> AudioEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    onUpdate = { onResult(VideoEditorBottomSheetRouteResultData.AudioUpdate(it)) },
                    onDelete = { onResult(VideoEditorBottomSheetRouteResultData.DeleteRenderItem(it)) }
                )

                is RenderData.CanvasItem.Image -> ImageRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    onUpdate = { onResult(VideoEditorBottomSheetRouteResultData.ImageUpdate(it)) },
                    onDelete = { onResult(VideoEditorBottomSheetRouteResultData.DeleteRenderItem(it)) }
                )

                is RenderData.CanvasItem.Text -> TextRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    onUpdate = { onResult(VideoEditorBottomSheetRouteResultData.TextCreateOrUpdate(it)) },
                    onDelete = { onResult(VideoEditorBottomSheetRouteResultData.DeleteRenderItem(it)) }
                )

                is RenderData.CanvasItem.Video -> VideoRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    onUpdate = { onResult(VideoEditorBottomSheetRouteResultData.VideoUpdate(it)) },
                    onDelete = { onResult(VideoEditorBottomSheetRouteResultData.DeleteRenderItem(it)) }
                )
            }
        }
    }
}