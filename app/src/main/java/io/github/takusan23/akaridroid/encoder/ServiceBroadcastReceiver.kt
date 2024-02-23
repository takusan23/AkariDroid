package io.github.takusan23.akaridroid.encoder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
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
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            IntentFilter().apply {
                actionList.forEach {
                    addAction(it)
                }
            },
            ContextCompat.RECEIVER_EXPORTED
        )
        awaitClose { context.unregisterReceiver(broadcastReceiver) }
    }

}