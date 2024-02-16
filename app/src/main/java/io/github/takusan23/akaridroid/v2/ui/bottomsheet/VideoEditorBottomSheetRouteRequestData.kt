package io.github.takusan23.akaridroid.v2.ui.bottomsheet

import io.github.takusan23.akaridroid.v2.RenderData

/** [VideoEditorBottomSheetRouter]で表示するためのデータ */
sealed interface VideoEditorBottomSheetRouteRequestData {

    /**
     * [RenderData.RenderItem]に対応する各編集ボトムシートを出す
     *
     * @param renderItem 編集するアイテム
     */
    data class OpenEditor(val renderItem: RenderData.RenderItem) : VideoEditorBottomSheetRouteRequestData

}