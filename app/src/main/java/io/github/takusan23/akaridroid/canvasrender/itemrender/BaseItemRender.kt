package io.github.takusan23.akaridroid.canvasrender.itemrender

import android.graphics.Canvas
import io.github.takusan23.akaridroid.RenderData

/**
 * [io.github.takusan23.akaridroid.v2.canvasrender.RenderData.RenderItem]を描画する。
 * 画像や文字、動画の描画はこのクラスを継承した Render によって行われます。
 *
 * # ライフサイクル
 * 例えば動画のハードウェアデコーダーは数に限りがあるので、
 * タイムライン上にある動画全てでハードウェアデコーダーを起動しようとすると数によっては足りない。
 * そういう場合は、[prepare]から[destroy]までで生存するようにする必要があります。
 *
 * - 準備
 *  - [prepare]
 * - 描画
 *  - [preDraw] -> [draw]
 * - 破棄
 *  - [destroy]
 *  - [destroy]後に再度必要であれば[prepare]に戻る
 */
abstract class BaseItemRender {

    /** 状態 */
    var currentLifecycleState = RenderLifecycleState.DESTROYED
        protected set

    /** レイヤー。タイムラインのレーン番号です */
    abstract val layerIndex: Int

    /**
     * 素材が使われる時間が来たときに一度呼ばれます。
     * [preDraw]より先に呼ばれます。
     * また、[destroy]したあと再度必要になった際にも呼ばれます。
     * 他の素材と一緒に並列で呼び出されます。
     */
    protected abstract suspend fun prepare()

    /**
     * 動画のフレーム（指定位置の動画の Bitmap）の取得など、時間がかかる場合は[preDraw]を利用してください。
     * [draw]の前に呼ばれて、並列で[preDraw]します。すべての呼び出しが完了した後に[draw]されます。
     * [draw]で重い処理をしない場合は、このメソッドを実装する必要はありません。
     */
    open suspend fun preDraw(durationMs: Long, currentPositionMs: Long) {
        // 実装は任意です
    }

    /**
     * 渡された[Canvas]に描画する。
     * サスペンド関数ですが、レイヤー順に直列で呼び出されるため、あんまり時間をかけないでください。
     *
     * @param canvas 描画先
     * @param durationMs 動画の合計時間
     * @param currentPositionMs 描画したい時間
     */
    abstract suspend fun draw(canvas: Canvas, durationMs: Long, currentPositionMs: Long)

    /**
     * 素材が使われる時間を抜けた。
     * ハードウェアデコーダー等、上限があるものに関してはここで開放してください。
     * 再度素材が必要な場合は[prepare]されます。
     */
    protected abstract fun destroy()

    /**
     * データが一緒かどうか返す
     *
     * @param renderItem 比較対象の[RenderData.CanvasItem]
     * @return 同じ場合は true
     */
    abstract suspend fun isEquals(renderItem: RenderData.CanvasItem): Boolean

    /**
     * 描画すべき時間を渡すので、描画すべきかどうかを返す
     * もしここで false を返した場合、[preDraw]、[draw]は呼ばれません
     *
     * @param currentPositionMs 描画する時間
     * @return true の場合描画する
     */
    abstract suspend fun isDisplayPosition(currentPositionMs: Long): Boolean

    /**
     * ライフサイクルをセットして、[prepare]と[destroy]の必要な方を呼び出す
     *
     * @param lifecycleState [RenderLifecycleState]
     */
    suspend fun setLifecycle(lifecycleState: RenderLifecycleState) {
        // 違うときのみ
        if (currentLifecycleState != lifecycleState) {
            when (lifecycleState) {
                RenderLifecycleState.DESTROYED -> destroy()
                RenderLifecycleState.PREPARED -> prepare()
            }
            currentLifecycleState = lifecycleState
        }
    }

    /** まだ利用可能なのか。[destroy]を呼び出した場合は再度[prepare]を呼ばないとなので。 */
    enum class RenderLifecycleState {
        /** 破棄済み、もしくはインスタンス生成後で準備したこと無い */
        DESTROYED,

        /** 準備済み */
        PREPARED
    }

}