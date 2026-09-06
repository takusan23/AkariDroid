package io.github.takusan23.akaridroid.ui.sheet

import androidx.compose.runtime.Composable
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.encoder.EncoderParameters
import io.github.takusan23.akaridroid.tool.AkaLinkTool
import io.github.takusan23.akaridroid.ui.component.AddRenderItemMenuResult

/**
 * BottomSheetRouter と OverlaySheetRouter で使われる共通部分
 *
 * @param videoEditorBottomSheetRouteRequestData ボトムシートの表示に必要なデータ
 * @param onSheetClose ボトムシート閉じたときに呼ばれる
 */
@Composable
fun VideoEditorSheetCommonRouter(
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
    onSheetClose: () -> Unit,
    onDefaultClick: () -> Unit,
    onMultiSelectClick: () -> Unit
) {
    when (videoEditorBottomSheetRouteRequestData) {

        // 編集画面を出す
        is VideoEditorBottomSheetRouteRequestData.OpenEditor -> when (videoEditorBottomSheetRouteRequestData.editRenderItem) {
            is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Audio -> AudioEditBottomSheet(
                renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.audio,
                onUpdate = {
                    onAudioUpdate(it)
                    onSheetClose()
                },
                onDelete = {
                    onDeleteItem(it)
                    onSheetClose()
                },
                onCloseClick = onSheetClose
            )

            is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Effect -> EffectRenderEditBottomSheet(
                renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.effect,
                onUpdate = {
                    onCanvasUpdate(it)
                    onSheetClose()
                },
                onDelete = {
                    onDeleteItem(it)
                    onSheetClose()
                },
                onCloseClick = onSheetClose
            )

            is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Image -> ImageRenderEditBottomSheet(
                renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.image,
                onUpdate = {
                    onCanvasUpdate(it)
                    onSheetClose()
                },
                onDelete = {
                    onDeleteItem(it)
                    onSheetClose()
                },
                onCloseClick = onSheetClose
            )

            is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Shader -> ShaderRenderEditBottomSheet(
                renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.shader,
                onUpdate = {
                    onCanvasUpdate(it)
                    onSheetClose()
                },
                onDelete = {
                    onDeleteItem(it)
                    onSheetClose()
                },
                onCloseClick = onSheetClose
            )

            is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Shape -> ShapeRenderEditBottomSheet(
                renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.shape,
                onUpdate = {
                    onCanvasUpdate(it)
                    onSheetClose()
                },
                onDelete = {
                    onDeleteItem(it)
                    onSheetClose()
                },
                onCloseClick = onSheetClose
            )

            is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.SwitchAnimation -> SwitchAnimationRenderEditBottomSheet(
                renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.switchAnimation,
                onUpdate = {
                    onCanvasUpdate(it)
                    onSheetClose()
                },
                onDelete = {
                    onDeleteItem(it)
                    onSheetClose()
                },
                onCloseClick = onSheetClose
            )


            is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Text -> TextRenderEditBottomSheet(
                renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.text,
                onUpdate = {
                    onCanvasUpdate(it)
                    onSheetClose()
                },
                onDelete = {
                    onDeleteItem(it)
                    onSheetClose()
                },
                onCloseClick = onSheetClose
            )

            is VideoEditorBottomSheetRouteRequestData.OpenEditor.EditRenderItemType.Video -> VideoRenderEditBottomSheet(
                renderItem = videoEditorBottomSheetRouteRequestData.editRenderItem.video,
                previewPositionMs = videoEditorBottomSheetRouteRequestData.editRenderItem.previewPositionMs,
                isProjectHdr = videoEditorBottomSheetRouteRequestData.editRenderItem.isProjectHdr,
                onOpenVideoInfo = onVideoInfoClick,
                onUpdate = {
                    onCanvasUpdate(it)
                    onSheetClose()
                },
                onDelete = {
                    onDeleteItem(it)
                    onSheetClose()
                },
                onCloseClick = onSheetClose
            )
        }

        // 動画情報編集画面
        is VideoEditorBottomSheetRouteRequestData.OpenVideoInfo -> VideoInfoEditorBottomSheet(
            renderData = videoEditorBottomSheetRouteRequestData.renderData,
            onUpdate = {
                onRenderDataUpdate(it)
                onSheetClose()
            },
            onCloseClick = onSheetClose
        )

        // あかりんく画面
        VideoEditorBottomSheetRouteRequestData.OpenAkaLink -> AkaLinkBottomSheet(
            onAkaLinkResult = { akaLinkResult ->
                onReceiveAkaLink(akaLinkResult)
                onSheetClose()
            }
        )

        // 動画保存画面
        is VideoEditorBottomSheetRouteRequestData.OpenEncode -> EncodeBottomSheet(
            videoSize = videoEditorBottomSheetRouteRequestData.videoSize,
            colorSpace = videoEditorBottomSheetRouteRequestData.colorSpace,
            onEncode = { fileName, parameters ->
                onEncode(fileName, parameters)
                onSheetClose()
            },
            onCloseClick = onSheetClose
        )

        // メニュー画面
        VideoEditorBottomSheetRouteRequestData.OpenMenu -> MenuBottomSheet(
            onVideoInfoClick = onVideoInfoClick,
            onEncodeClick = onEncodeClick,
            onSaveVideoFrameClick = {
                onSaveVideoFrameClick()
                onSheetClose()
            },
            onTimeLineReset = onTimeLineReset,
            onSettingClick = onSettingClick,
            onCloseClick = onSheetClose
        )

        // 素材追加画面
        VideoEditorBottomSheetRouteRequestData.OpenAddRenderItem -> AddRenderItemBottomSheet(
            onAddRenderItemResult = onAddRenderItemResult,
            onCloseClick = onSheetClose
        )

        // タイムラインのモード変更
        VideoEditorBottomSheetRouteRequestData.OpenTimeLineModeChange -> TimeLineModeChangeBottomSheet(
            onDefaultClick = {
                onDefaultClick()
                onSheetClose()
            },
            onMultiSelectClick = {
                onMultiSelectClick()
                onSheetClose()
            },
            onCloseClick = onSheetClose
        )
    }
}