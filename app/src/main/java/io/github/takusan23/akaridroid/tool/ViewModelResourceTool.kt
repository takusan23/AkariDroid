package io.github.takusan23.akaridroid.tool

import android.content.Context

/**
 * ViewModel から Context.getString するための関数がある
 * TODO そもそも ViewModel から getString するべきではない。Compose コードまで移動させることを検討
 *
 * Koin DI ライブラリ経由でこのクラスのインスタンスが取得できます
 *
 * @param context Koin 経由
 */
class ViewModelResourceTool(private val context: Context) {

    /** [Context.getString]を呼び出す */
    fun getString(resId: Int) = context.getString(resId)

}