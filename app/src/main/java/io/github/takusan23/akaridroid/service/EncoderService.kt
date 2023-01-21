package io.github.takusan23.akaridroid.service

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.takusan23.akaricore.AkariCore
import io.github.takusan23.akaricore.data.AudioEncoderData
import io.github.takusan23.akaricore.data.VideoEncoderData
import io.github.takusan23.akaricore.data.VideoFileData
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.data.AkariProjectData
import io.github.takusan23.akaridroid.service.tool.ServiceBroadcastReceiver
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import io.github.takusan23.akaridroid.ui.tool.AkariCanvas
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.File
import java.lang.ref.WeakReference

/** エンコーダーサービス */
class EncoderService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val localBinder = LocalBinder(this)

    private val _isRunningEncode = MutableStateFlow(false)

    /** エンコード中かどうか */
    val isRunningEncode = _isRunningEncode.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        // ブロードキャスト
        ServiceBroadcastReceiver
            .collectReceivedBroadcast(this, EncoderServiceBroadcastAction.values().map { it.action })
            .onEach { action ->
                when (EncoderServiceBroadcastAction.resolve(action)) {
                    EncoderServiceBroadcastAction.SERVICE_STOP -> stop()
                }
            }.launchIn(scope)
    }

    override fun onBind(intent: Intent?): IBinder = localBinder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }

    /** [AkariProjectData]をもとにエンコードを行う */
    fun encodeAkariProject(akariProjectData: AkariProjectData) {
        scope.launch {
            // プロジェクトをロードしてエンコードする
            encodeAkariCore(akariProjectData)
        }
    }

    /** 処理を止める */
    fun stop() {
        scope.coroutineContext.cancelChildren()
    }

    /**
     * エンコードを行う。
     * [AkariCore]は別モジュールに実装してあります。
     *
     * @param akariProjectData [AkariProjectData]
     */
    private suspend fun encodeAkariCore(akariProjectData: AkariProjectData) {
        val videoFile = File(akariProjectData.videoFilePath!!)
        val resultFile = File(getExternalFilesDir(null), "result_${System.currentTimeMillis()}.mp4").apply {
            delete()
            createNewFile()
        }
        val tempFolder = File(getExternalFilesDir(null), "temp").apply { mkdir() }

        // エンコーダーの値をそれぞれセットする
        val outputFormat = akariProjectData.videoOutputFormat
        val codec = outputFormat.videoCodec
        val videoEncoder = VideoEncoderData(
            codecName = codec.videoMediaCodecMimeType,
            height = outputFormat.videoHeight,
            width = outputFormat.videoWidth,
            bitRate = outputFormat.bitRate,
            frameRate = outputFormat.frameRate,
        )
        val audioEncoder = AudioEncoderData(codecName = codec.audioMediaCodecMimeType)
        val videoFileData = VideoFileData(videoFile = videoFile, tempWorkFolder = tempFolder, containerFormat = codec.containerFormat.mediaMuxerVal, outputFile = resultFile)
        val akariCore = AkariCore(videoFileData, videoEncoder, audioEncoder)

        // エンコードを開始する。フォアグラウンドサービスにしてバインドが解除されても動くようにする。
        _isRunningEncode.value = true
        createOrUpdateForegroundNotification(title = "エンコード中です", text = "しばらくお待ちください、がんばってます。", isEncoding = true)

        try {
            // エンコーダーを開始する
            withContext(Dispatchers.Default) {
                akariCore.start { positionMs ->
                    // this は Canvas
                    // 動画の上に重ねるCanvasを描画する
                    AkariCanvas.render(this, akariProjectData.canvasElementList)
                }
            }
            // 動画フォルダへコピーする
            MediaStoreTool.copyToVideoFolder(this, resultFile)
            // コピー後のファイルを消す
            resultFile.delete()
        } catch (e: Exception) {
            // TODO キャンセル時
        } finally {
            // 終了。フォアグラウンドを解除する
            _isRunningEncode.value = false
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
    }

    /**
     * サービスをフォアグラウンドに昇格させる。そのための通知を作成する。
     *
     * @param title タイトル
     * @param text 通知本文
     */
    private fun createOrUpdateForegroundNotification(title: String = "サービス起動中", text: String = "あかりどろいど より", isEncoding: Boolean = false) {
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
            if (isEncoding) {
                addAction(R.drawable.ic_outline_close_24, "エンコード終了", PendingIntent.getBroadcast(this@EncoderService, 1, Intent(EncoderServiceBroadcastAction.SERVICE_STOP.action), flags))
            }
        }.build()
        startForeground(NOTIFICATION_ID, notification)
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
            fun resolve(action: String): EncoderServiceBroadcastAction = values().first { it.action == action }
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

        /** サービスをフォアグラウンド化するか。エンコード中の場合はフォアグラウンド化して処理を継続させる */

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