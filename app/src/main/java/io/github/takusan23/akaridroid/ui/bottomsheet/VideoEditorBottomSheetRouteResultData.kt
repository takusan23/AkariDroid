package io.github.takusan23.akaridroid.ui.bottomsheet

import io.github.takusan23.akaridroid.RenderData

/** [VideoEditorBottomSheetRouter]の返り値 */
sealed interface VideoEditorBottomSheetRouteResultData {

    /** テキストの追加か更新 */
    data class TextCreateOrUpdate(val text: RenderData.CanvasItem.Text) : VideoEditorBottomSheetRouteResultData

    /** 動画素材の更新 */
    data class VideoUpdate(val video: RenderData.CanvasItem.Video) : VideoEditorBottomSheetRouteResultData

    /** 音声素材の更新 */
    data class AudioUpdate(val audio: RenderData.AudioItem.Audio) : VideoEditorBottomSheetRouteResultData

    /** 画像素材の更新 */
    data class ImageUpdate(val image: RenderData.CanvasItem.Image) : VideoEditorBottomSheetRouteResultData

    /** 動画情報の更新 */
    data class VideoInfoUpdate(val renderData: RenderData) : VideoEditorBottomSheetRouteResultData

    /** キャンバス要素や音声要素の削除 */
    data class DeleteRenderItem(val renderItem: RenderData.RenderItem) : VideoEditorBottomSheetRouteResultData

}