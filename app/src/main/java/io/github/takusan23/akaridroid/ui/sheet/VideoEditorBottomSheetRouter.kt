package io.github.takusan23.akaridroid.ui.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.encoder.EncoderParameters
import io.github.takusan23.akaridroid.tool.AkaLinkTool
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult

/**
 * 動画編集画面で使うボトムシートを出す画面
 * 大画面レイアウトの時は OverlaySheetRouter の方になる
 *
 * @param videoEditorBottomSheetRouteRequestData ボトムシートの表示に必要なデータ
 * @param onClose ボトムシート閉じたときに呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorBottomSheetRouter(
    videoEditorBottomSheetRouteRequestData: VideoEditorBottomSheetRouteRequestData,
    onAudioUpdate: (RenderData.AudioItem) -> Unit,
    onCanvasUpdate: (RenderData.CanvasItem) -> Unit,
    onDeleteItem: (RenderData.RenderItem) -> Unit,
    onAddRenderItemResult: (AddRenderItemMenuResult) -> Unit,
    onReceiveAkaLink: (AkaLinkTool.AkaLinkResult) -> Unit,
    onRenderDataUpdate: (RenderData) -> Unit,
    onEncode: (String, EncoderParameters) -> Unit,
    onVideoInfoClick: () -> Unit,
    onEncodeClick: () -> Unit,
    onSaveVideoFrameClick: () -> Unit,
    onTimeLineReset: () -> Unit,
    onSettingClick: () -> Unit,
    onStartAkaLink: () -> Unit,
    onClose: () -> Unit,
    onDefaultClick: () -> Unit,
    onMultiSelectClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        VideoEditorSheetCommonRouter(
            videoEditorBottomSheetRouteRequestData = videoEditorBottomSheetRouteRequestData,
            onAudioUpdate = onAudioUpdate,
            onCanvasUpdate = onCanvasUpdate,
            onDeleteItem = onDeleteItem,
            onAddRenderItemResult = onAddRenderItemResult,
            onReceiveAkaLink = onReceiveAkaLink,
            onRenderDataUpdate = onRenderDataUpdate,
            onEncode = onEncode,
            onVideoInfoClick = onVideoInfoClick,
            onEncodeClick = onEncodeClick,
            onSaveVideoFrameClick = onSaveVideoFrameClick,
            onTimeLineReset = onTimeLineReset,
            onSettingClick = onSettingClick,
            onStartAkaLink = onStartAkaLink,
            onClose = onClose,
            onDefaultClick = onDefaultClick,
            onMultiSelectClick = onMultiSelectClick
        )
    }
}