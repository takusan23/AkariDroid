package io.github.takusan23.akaridroid.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.takusan23.akaricore.AkariCore
import io.github.takusan23.akaricore.data.AudioEncoderData
import io.github.takusan23.akaricore.data.VideoEncoderData
import io.github.takusan23.akaricore.data.VideoFileData
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.data.AkariProjectData
import io.github.takusan23.akaridroid.manager.VideoEditProjectManager
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import io.github.takusan23.akaridroid.ui.tool.AkariCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** エンコーダーサービス */
class EncoderService : LifecycleService() {

    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val videoEditProjectManager by lazy { VideoEditProjectManager(this) }

    override fun onCreate() {
        super.onCreate()
        // フォアグラウンドサービスに昇格させる
        createNotification()
        // ブロードキャスト
        ServiceBroadcastReceiver(
            context = this,
            lifecycleOwner = this,
            actionList = EncoderServiceBroadcastAction.values().map { it.action }
        ) {
            when (EncoderServiceBroadcastAction.resolve(it)) {
                EncoderServiceBroadcastAction.SERVICE_STOP -> stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        lifecycleScope.launch {
            // プロジェクトをロードしてエンコードする
            val akariProjectData = videoEditProjectManager.loadProjectData(intent?.getStringExtra(INTENT_PROJECT_ID_KEY)!!)
            akariCoreEncode(akariProjectData)
            // 終了する
            stopSelf()
        }
        return START_NOT_STICKY
    }

    /**
     * エンコードを行う。
     * [io.github.takusan23.akaricore.AkariCore]は別モジュールに実装してあります。
     *
     * @param akariProjectData [AkariProjectData]
     */
    private suspend fun akariCoreEncode(akariProjectData: AkariProjectData) {
        val videoFile = File("${getExternalFilesDir(null)!!.path}/videos/sample.mp4")
        val resultFile = File(getExternalFilesDir(null), "result_${System.currentTimeMillis()}.mp4").apply {
            delete()
            createNewFile()
        }
        val tempFolder = File(getExternalFilesDir(null), "temp").apply { mkdir() }

        val videoEncoder = VideoEncoderData(codecName = MediaFormat.MIMETYPE_VIDEO_AVC)
        val audioEncoder = AudioEncoderData(codecName = MediaFormat.MIMETYPE_AUDIO_AAC)
        val videoFileData = VideoFileData(videoFile = videoFile, tempWorkFolder = tempFolder, containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4, outputFile = resultFile)
        val akariCore = AkariCore(videoFileData, videoEncoder, audioEncoder)

        withContext(Dispatchers.Default) {
            // エンコーダーを開始する
            akariCore.start { positionMs ->
                // this は Canvas
                // 動画の上に重ねるCanvasを描画する
                AkariCanvas.render(this, akariProjectData.canvasElementList)
            }
        }
        // 動画フォルダへコピーする
        MediaStoreTool.copyToVideoFolder(this@EncoderService, resultFile)
    }

    /** 通知を作る */
    private fun createNotification() {
        val channelId = "service_encoder_running"
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val notificationChannel = NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW).apply {
                setName("エンコーダーサービス実行中通知")
            }.build()
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val notification = NotificationCompat.Builder(this, channelId).apply {
            setContentTitle("動画のエンコード中です")
            setContentText("しばらくお待ち下さい。")
            setSmallIcon(R.drawable.akari_droid_icon)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            addAction(R.drawable.ic_outline_close_24, "強制終了", PendingIntent.getBroadcast(this@EncoderService, 1, Intent(EncoderServiceBroadcastAction.SERVICE_STOP.action), flags))
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

    companion object {

        /** 通知ID */
        private const val NOTIFICATION_ID = 4545

        /** プロジェクトID */
        private const val INTENT_PROJECT_ID_KEY = "project_id"

        /**
         * サービスを起動するインテントを作成
         * IntentにJSONを入れるともれなく上限に引っかかると思うので、ファイルに保存してから行う
         *
         * @param context [Context]
         * @param projectId プロジェクトID
         */
        fun createIntent(context: Context, projectId: String): Intent {
            return Intent(context, EncoderService::class.java).apply {
                putExtra(INTENT_PROJECT_ID_KEY, projectId)
            }
        }
    }
}