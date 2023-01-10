package io.github.takusan23.akaridroid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * サービスに置くブロードキャストレシーバー
 *
 * @param context [Context]
 * @param lifecycleOwner ブロードキャストレシーバーを自動で登録、登録破棄を行うため
 * @param actionList ブロードキャストを登録するユニークな文字列。サービス終了用など
 * @param onReceive ブロードキャストを受け取ると呼ばれる
 */
class ServiceBroadcastReceiver(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val actionList: List<String>,
    private val onReceive: (String) -> Unit
) {

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.apply(onReceive)
        }
    }

    val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            context.registerReceiver(broadcastReceiver, IntentFilter().apply {
                actionList.forEach {
                    addAction(it)
                }
            })
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            context.unregisterReceiver(broadcastReceiver)
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

}