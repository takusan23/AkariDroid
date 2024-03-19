package io.github.takusan23.akaridroid.ui.bottomsheet

import android.net.Uri
import io.github.takusan23.akaridroid.RenderData

/** [VideoEditorBottomSheetRouter]の返り値 */
sealed interface VideoEditorBottomSheetRouteResultData {

    /** 動画情報の更新 */
    data class UpdateVideoInfo(val renderData: RenderData) : VideoEditorBottomSheetRouteResultData

    /** キャンバス素材の更新 */
    data class UpdateCanvasItem(val renderData: RenderData.CanvasItem) : VideoEditorBottomSheetRouteResultData

    /** 音声素材の更新 */
    data class UpdateAudio(val audio: RenderData.AudioItem.Audio) : VideoEditorBottomSheetRouteResultData

    /** キャンバス要素や音声要素の削除 */
    data class DeleteRenderItem(val renderItem: RenderData.RenderItem) : VideoEditorBottomSheetRouteResultData

    /** あかりんく で素材を受け取った */
    data class ReceiveAkaLink(val uri: Uri) : VideoEditorBottomSheetRouteResultData
}