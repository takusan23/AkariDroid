package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.encoder.EncoderParameters
import io.github.takusan23.akaridroid.tool.AkaLinkTool
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult

/**
 * 動画編集画面で使うボトムシートを出す画面
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
    onTimeLineReset: () -> Unit,
    onSettingClick: () -> Unit,
    onStartAkaLink: () -> Unit,
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
                    onUpdate = {
                        onAudioUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is RenderData.CanvasItem.Image -> ImageRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is RenderData.CanvasItem.Text -> TextRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is RenderData.CanvasItem.Video -> VideoRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    previewPositionMs = videoEditorBottomSheetRouteRequestData.previewPositionMs,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is RenderData.CanvasItem.Shape -> ShapeRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is RenderData.CanvasItem.Shader -> ShaderRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is RenderData.CanvasItem.SwitchAnimation -> SwitchAnimationRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.renderItem,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )
            }

            // 動画情報編集画面
            is VideoEditorBottomSheetRouteRequestData.OpenVideoInfo -> VideoInfoEditorBottomSheet(
                renderData = videoEditorBottomSheetRouteRequestData.renderData,
                onUpdate = {
                    onRenderDataUpdate(it)
                    onClose()
                }
            )

            // あかりんく画面
            VideoEditorBottomSheetRouteRequestData.OpenAkaLink -> AkaLinkBottomSheet(
                onAkaLinkResult = { akaLinkResult ->
                    onReceiveAkaLink(akaLinkResult)
                    onClose()
                }
            )

            // 動画保存画面
            is VideoEditorBottomSheetRouteRequestData.OpenEncode -> EncodeBottomSheet(
                videoSize = videoEditorBottomSheetRouteRequestData.videoSize,
                onEncode = { fileName, parameters ->
                    onEncode(fileName, parameters)
                    onClose()
                }
            )

            // メニュー画面
            VideoEditorBottomSheetRouteRequestData.OpenMenu -> MenuBottomSheet(
                onVideoInfoClick = onVideoInfoClick,
                onEncodeClick = onEncodeClick,
                onTimeLineReset = onTimeLineReset,
                onSettingClick = onSettingClick
            )

            // 素材追加画面
            VideoEditorBottomSheetRouteRequestData.OpenAddRenderItem -> AddRenderItemBottomSheet(
                onAddRenderItemResult = onAddRenderItemResult
            )
        }
    }
}