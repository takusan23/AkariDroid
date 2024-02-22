package io.github.takusan23.akaridroid.v2.encoder

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.v2.RenderData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

/** エンコーダーサービス */
class EncoderService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val localBinder = LocalBinder(this)

    /** エンコードキャンセル用 [Job] */
    private var encoderJob: Job? = null

    private val _isRunningEncode = MutableStateFlow(false)

    /** エンコード中かどうか */
    val isRunningEncode = _isRunningEncode.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        // ブロードキャスト
        scope.launch {
            ServiceBroadcastReceiver.collectReceivedBroadcast(this@EncoderService, EncoderServiceBroadcastAction.entries.map { it.action })
                .collect { action ->
                    when (EncoderServiceBroadcastAction.resolve(action)) {
                        EncoderServiceBroadcastAction.SERVICE_STOP -> stop()
                    }
                }
        }
    }

    override fun onBind(intent: Intent?): IBinder = localBinder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    /** [RenderData]をもとにエンコードを行う */
    fun encodeAkariCore(renderData: RenderData, projectFolder: File) {
        encoderJob = scope.launch {
            try {
                // フォアグラウンドサービスに昇格させる
                _isRunningEncode.value = true
                createOrUpdateForegroundNotification()

                // エンコード
                AkariCoreEncoder.encode(
                    context = this@EncoderService,
                    projectFolder = projectFolder,
                    renderData = renderData
                )
            } finally {
                // 完了時はフォアグラウンドサービスを通常サービスに
                _isRunningEncode.value = false
                ServiceCompat.stopForeground(this@EncoderService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
        }
    }

    /** 処理を止める */
    fun stop() {
        scope.launch {
            encoderJob?.cancelAndJoin()
        }
    }

    /**
     * サービスをフォアグラウンドに昇格させる。そのための通知を作成する。
     *
     * @param title タイトル
     * @param text 通知本文
     */
    private fun createOrUpdateForegroundNotification(
        title: String = "サービス起動中",
        text: String = "あかりどろいど より"
    ) {
        val channelId = "service_encoder_running"
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val notificationChannel = NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW).apply {
                setName("エンコーダーサービス実行中通知")
            }.build()
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val notification = NotificationCompat.Builder(this, channelId).apply {
            setContentTitle(title)
            setContentText(text)
            setSmallIcon(R.drawable.akari_droid_icon)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            addAction(R.drawable.ic_outline_close_24, "エンコード終了", PendingIntent.getBroadcast(this@EncoderService, 1, Intent(EncoderServiceBroadcastAction.SERVICE_STOP.action), flags))
        }.build()
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundServiceType)
    }

    /** ブロードキャストのアクション列挙型 */
    private enum class EncoderServiceBroadcastAction(val action: String) {

        /** サービス終了 */
        SERVICE_STOP("io.github.takusan23.akaridroid.service.SERVICE_STOP");

        companion object {

            /**
             * [EncoderServiceBroadcastAction.action]から値を返す
             *
             * @param action
             */
            fun resolve(action: String): EncoderServiceBroadcastAction = entries.first { it.action == action }
        }
    }

    private class LocalBinder(service: EncoderService) : Binder() {
        val serviceRef = WeakReference(service)
        val service: EncoderService
            get() = serviceRef.get()!!
    }

    companion object {

        /** 通知ID */
        private const val NOTIFICATION_ID = 4545

        /**
         * サービスとバインドしてサービスのインスタンスを取得する
         *
         * @param context [Context]
         * @param lifecycleOwner ライフサイクルオーナー
         */
        fun bindEncoderService(context: Context, lifecycleOwner: LifecycleOwner) = callbackFlow {
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val encoderService = (service as LocalBinder).service
                    trySend(encoderService)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    trySend(null)
                }
            }
            // ライフサイクルを監視してバインド、バインド解除する
            val lifecycleObserver = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    val intent = Intent(context, EncoderService::class.java)
                    context.startService(intent)
                    context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
                }

                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    context.unbindService(serviceConnection)
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            awaitClose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
        }
    }
}