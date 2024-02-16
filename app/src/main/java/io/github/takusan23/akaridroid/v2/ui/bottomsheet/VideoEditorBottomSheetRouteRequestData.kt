package io.github.takusan23.akaridroid.v2.ui.bottomsheet

import io.github.takusan23.akaridroid.v2.RenderData

/** [VideoEditorBottomSheetRouter]で表示するためのデータ */
sealed interface VideoEditorBottomSheetRouteRequestData {

    /**
     * [RenderData.RenderItem]の編集ボトムシートを出す
     *
     * @param renderItem 編集するアイテム
     * @param isEdit 編集モードは true
     */
    data class OpenEditor(
        val renderItem: RenderData.RenderItem,
        val isEdit: Boolean = false
    ) : VideoEditorBottomSheetRouteRequestData

}