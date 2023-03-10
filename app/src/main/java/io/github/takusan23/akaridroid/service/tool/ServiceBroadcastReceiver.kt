package io.github.takusan23.akaridroid.service.tool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/** ブロードキャスト */
object ServiceBroadcastReceiver {

    /**
     * ブロードキャストを購読する。コルーチンスコープが破棄されたら自動で解除される。
     * Flowで流れてくる値は受信したブロードキャストの[Intent.getAction]
     *
     * @param context [Context]
     * @param actionList 操作リスト
     */
    fun collectReceivedBroadcast(context: Context, actionList: List<String>) = callbackFlow<String> {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.action?.also { action -> trySend(action) }
            }
        }
        context.registerReceiver(broadcastReceiver, IntentFilter().apply {
            actionList.forEach {
                addAction(it)
            }
        })
        awaitClose { context.unregisterReceiver(broadcastReceiver) }
    }

}