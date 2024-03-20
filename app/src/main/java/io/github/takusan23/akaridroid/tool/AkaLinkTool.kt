package io.github.takusan23.akaridroid.tool

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

/**
 * あかりんく（AkaLink）関連の処理があるクラス
 * この仕組みを利用することで、以下の仕様に準拠したアプリを作ると、他のアプリで素材を作ってタイムラインに追加ができます。
 *
 * # 外部アプリの作り方
 * README.md に書く
 */
object AkaLinkTool {

    private val TAG = AkaLinkTool::class.java.simpleName

    /** [Intent]のアクション。 */
    val ACTION_START_AKALINK = "io.github.takusan23.akaridroid.ACTION_START_AKALINK"

    /**
     * [Context.getExternalFilesDir]の中にこの名前でフォルダを作る。
     * この中に外部連携アプリ側が読み書きするファイルを作成する。
     */
    private const val AKALINK_FOLDER_NAME = "akalink"

    /** 外部連携用の[Intent]を作る */
    fun createAkaLinkStartIntent(context: Context): AkaLinkIntentData {
        // 外部連携アプリが素材を保存できるように、こちらでファイルを作成したのち、
        // Uri を作成し Intent に乗せて共有する
        val folder = context.getExternalFilesDir(null)!!.resolve(AKALINK_FOLDER_NAME).apply { mkdir() }
        val externalShareFile = folder.resolve("akalink_${System.currentTimeMillis()}").apply { createNewFile() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", externalShareFile)
        val intent = Intent(ACTION_START_AKALINK, uri).apply {
            // 読み書き権限
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        return AkaLinkIntentData(intent, uri, externalShareFile)
    }

    /**
     * 外部連携で、[androidx.activity.compose.rememberLauncherForActivityResult]でアプリに戻ってきた時。
     * もし失敗していたらデータを消す処理もある。
     *
     * @param context [Context]
     * @param akaLinkIntentData [createAkaLinkStartIntent]で作ったデータ
     * @param resultIntent 帰ってきた[Intent]
     * @param resultCode 帰ってきた[Int]
     */
    suspend fun resolveAkaLinkResultIntent(
        context: Context,
        resultCode: Int,
        resultIntent: Intent?,
        akaLinkIntentData: AkaLinkIntentData?
    ): AkaLinkResult? {

        // 早期 return で失敗判定
        akaLinkIntentData ?: return null
        if (resultIntent == null) {
            akaLinkIntentData.delete()
            return null
        }

        val fileSize = akaLinkIntentData.file.length()
        val mimeType = resultIntent.type
        val fileNameOrNull = resultIntent.getStringExtra(Intent.EXTRA_TITLE)

        // 早期 return で失敗判定
        // Intent が成功していない、ファイルが空っぽ、MIME-Type が不明は早期 return する
        if (resultCode != Activity.RESULT_OK) {
            akaLinkIntentData.delete()
            return null
        }
        if (mimeType == null) {
            akaLinkIntentData.delete()
            return null
        }
        if (fileSize <= 0) {
            akaLinkIntentData.delete()
            return null
        }

        // おっけー
        Log.d(TAG, "AkaLink file receive. MIME-Type = $mimeType , fileSize = $fileSize , fileNameOrNull = $fileNameOrNull")

        // リネームする場合は
        // TODO Android 8 and later... renameTo でも良かった？
        val filePath = if (fileNameOrNull != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            withContext(Dispatchers.IO) {
                context.getAkaLinkFolder().resolve(fileNameOrNull)
                    .also { newFile -> Files.move(akaLinkIntentData.file.toPath(), newFile.toPath()) }
                    .path
            }
        } else {
            akaLinkIntentData.file.path
        }

        // AkaLinkResult にする
        return when {
            mimeType.startsWith("image/") -> AkaLinkResult.Image(filePath)
            mimeType.startsWith("audio/") -> AkaLinkResult.Audio(filePath)
            mimeType.startsWith("video/") -> AkaLinkResult.Video(filePath)
            else -> null
        }
    }

    /** [AKALINK_FOLDER_NAME]のフォルダを取得する */
    private fun Context.getAkaLinkFolder(): File = this.getExternalFilesDir(null)!!.resolve(AKALINK_FOLDER_NAME)

    /** ファイルを破棄する */
    private suspend fun AkaLinkIntentData.delete() {
        withContext(Dispatchers.IO) {
            file.delete()
        }
    }

    /**
     * Intent と渡したファイル
     *
     * @param intent Intent
     * @param file 外部アプリが読み書きするファイル
     * @param uri FileProvider が払い出した File に向いた Uri
     */
    data class AkaLinkIntentData(
        val intent: Intent,
        val uri: Uri,
        val file: File
    )

    /** 外部連携結果 */
    sealed interface AkaLinkResult {

        /**
         * 保存先のファイルパス。[File]で利用可能。
         * [io.github.takusan23.akaridroid.RenderData.FilePath.File]
         */
        val filePath: String

        /** 画像 */
        data class Image(override val filePath: String) : AkaLinkResult

        /** 音声 */
        data class Audio(override val filePath: String) : AkaLinkResult

        /** 動画 */
        data class Video(override val filePath: String) : AkaLinkResult
    }
}