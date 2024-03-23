package io.github.takusan23.akaridroid.ui.bottomsheet

import io.github.takusan23.akaridroid.RenderData

/** [VideoEditorBottomSheetRouter]で表示するためのデータ */
sealed interface VideoEditorBottomSheetRouteRequestData {

    /**
     * [RenderData.RenderItem]に対応する各編集ボトムシートを出す
     *
     * @param renderItem 編集するアイテム
     */
    data class OpenEditor(val renderItem: RenderData.RenderItem) : VideoEditorBottomSheetRouteRequestData

    /**
     * 動画情報の編集画面を開く
     *
     * @param renderData [RenderData]
     */
    data class OpenVideoInfo(val renderData: RenderData) : VideoEditorBottomSheetRouteRequestData

    /**
     * あかりんく 画面を開く
     */
    data object OpenAkaLink : VideoEditorBottomSheetRouteRequestData

    /** 動画を保存、エンコード画面を開く */
    data object OpenEncode : VideoEditorBottomSheetRouteRequestData
}