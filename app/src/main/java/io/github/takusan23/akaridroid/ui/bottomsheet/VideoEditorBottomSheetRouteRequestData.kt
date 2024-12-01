package io.github.takusan23.akaridroid.ui.bottomsheet

import io.github.takusan23.akaridroid.RenderData

/** [VideoEditorBottomSheetRouter]で表示するためのデータ */
sealed interface VideoEditorBottomSheetRouteRequestData {

    /**
     * [RenderData.RenderItem]に対応する各編集ボトムシートを出す
     *
     * @param editRenderItem 編集するアイテム
     */
    data class OpenEditor(val editRenderItem: EditRenderItemType) : VideoEditorBottomSheetRouteRequestData {

        /** [RenderData.RenderItem]をラップしてるだけ、追加で値を渡したかった。 */
        sealed interface EditRenderItemType {

            /** 音声 */
            data class Audio(val audio: RenderData.AudioItem.Audio) : EditRenderItemType

            /** エフェクト */
            data class Effect(val effect: RenderData.CanvasItem.Effect) : EditRenderItemType

            /** 画像 */
            data class Image(val image: RenderData.CanvasItem.Image) : EditRenderItemType

            /** シェーダー */
            data class Shader(val shader: RenderData.CanvasItem.Shader) : EditRenderItemType

            /** 図形 */
            data class Shape(val shape: RenderData.CanvasItem.Shape) : EditRenderItemType

            /** 切り替えアニメーション */
            data class SwitchAnimation(val switchAnimation: RenderData.CanvasItem.SwitchAnimation) : EditRenderItemType

            /** テキスト */
            data class Text(val text: RenderData.CanvasItem.Text) : EditRenderItemType

            /**
             * 動画
             * @param previewPositionMs プレビューの時間
             * @param isProjectHdr プロジェクトで 10Bit HDR が有効の場合
             */
            data class Video(val video: RenderData.CanvasItem.Video, val previewPositionMs: Long, val isProjectHdr: Boolean) : EditRenderItemType
        }
    }

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

    /**
     * 動画を保存、エンコード画面を開く
     *
     * @param videoSize 動画の縦横サイズ
     */
    data class OpenEncode(val videoSize: RenderData.Size) : VideoEditorBottomSheetRouteRequestData

    /** メニューを開く */
    data object OpenMenu : VideoEditorBottomSheetRouteRequestData

    /** 追加メニューを開く */
    data object OpenAddRenderItem : VideoEditorBottomSheetRouteRequestData
}