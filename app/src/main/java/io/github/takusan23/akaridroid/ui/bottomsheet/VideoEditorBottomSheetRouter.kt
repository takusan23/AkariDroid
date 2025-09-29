package io.github.takusan23.akaridroid.ui.bottomsheet

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
    onSaveVideoFrameClick: () -> Unit,
    onTimeLineReset: () -> Unit,
    onSettingClick: () -> Unit,
    onStartAkaLink: () -> Unit,
    onClose: () -> Unit,
    onDefaultClick: () -> Unit,
    onMultiSelectClick: () -> Unit
) {

    ModalBottomSheet(onDismissRequest = onClose) {

        when (videoEditorBottomSheetRouteRequestData) {

            // 編集画面を出す
            is VideoEditorBottomSheetRouteRequestData.OpenEditor -> when (videoEditorBottomSheetRouteRequestData.editRenderItem) {
                is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Audio -> AudioEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.audio,
                    onUpdate = {
                        onAudioUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Effect -> EffectRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.effect,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Image -> ImageRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.image,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Shader -> ShaderRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.shader,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Shape -> ShapeRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.shape,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.SwitchAnimation -> SwitchAnimationRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.switchAnimation,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )


                is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Text -> TextRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.text,
                    onUpdate = {
                        onCanvasUpdate(it)
                        onClose()
                    },
                    onDelete = {
                        onDeleteItem(it)
                        onClose()
                    }
                )

                is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Video -> VideoRenderEditBottomSheet(
                    renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.video,
                    previewPositionMs = videoEditorBottomSheetRouteRequestData.editRenderItem.previewPositionMs,
                    isProjectHdr = videoEditorBottomSheetRouteRequestData.editRenderItem.isProjectHdr,
                    onOpenVideoInfo = onVideoInfoClick,
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
                colorSpace = videoEditorBottomSheetRouteRequestData.colorSpace,
                onEncode = { fileName, parameters ->
                    onEncode(fileName, parameters)
                    onClose()
                }
            )

            // メニュー画面
            VideoEditorBottomSheetRouteRequestData.OpenMenu -> MenuBottomSheet(
                onVideoInfoClick = onVideoInfoClick,
                onEncodeClick = onEncodeClick,
                onSaveVideoFrameClick = onSaveVideoFrameClick,
                onTimeLineReset = onTimeLineReset,
                onSettingClick = onSettingClick
            )

            // 素材追加画面
            VideoEditorBottomSheetRouteRequestData.OpenAddRenderItem -> AddRenderItemBottomSheet(
                onAddRenderItemResult = onAddRenderItemResult
            )

            // タイムラインのモード変更
            VideoEditorBottomSheetRouteRequestData.OpenTimeLineModeChange -> TimeLineModeChangeBottomSheet(
                onDefaultClick = {
                    onDefaultClick()
                    onClose()
                },
                onMultiSelectClick = {
                    onMultiSelectClick()
                    onClose()
                }
            )
        }
    }
}