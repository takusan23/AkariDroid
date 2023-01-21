package io.github.takusan23.akaricore.data

import android.content.Context
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 動画ファイル関連、コンテナフォーマットとか。今回は多分使わない。
 * 作成後は MediaStore を通して端末の動画フォルダへ保存されます
 *
 * @param context [Context]
 * @param videoUri 動画Uri
 * @param tempWorkFolder 一時的にファイルを置くためそのためのフォルダ、終了後に削除されます
 * @param containerFormat フォーマット [android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4] など
 * @param resultName ファイル名
 */
class VideoUriData(
    private val context: Context,
    private val videoUri: Uri,
    private val resultName: String,
    private val folderName: String = "kotori",
    override val tempWorkFolder: File,
    override val containerFormat: Int,
) : VideoFileInterface {

    override val videoFile: File
        get() = File(tempWorkFolder, READ_VIDEO_URI_NAME).apply { createNewFile() }

    override val outputFile: File
        get() = File(tempWorkFolder, resultName).apply { createNewFile() }

    override val audioAssetFileList: List<File>
        get() = emptyList()

    override suspend fun prepare() {
        super.prepare()
        copyFileFromUri(videoUri, videoFile)
    }

    override suspend fun destroy() {
        super.destroy()
        // MediaStoreに登録してコピーしてから消す
        val mimeType = if (containerFormat == MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM) {
            "video/webm"
        } else {
            "video/mp4"
        }
        copyDeviceMovieFolder(
            context = context,
            copyFile = outputFile,
            folderName = folderName,
            fileName = resultName,
            mimeType = mimeType
        )
        tempWorkFolder.deleteRecursively()
    }

    /**
     * Uriなファイルを内部固有ストレージへ保存する。Uriは扱えないので
     *
     * @param uri コピーするファイルの[Uri]
     * @param targetFile コピー先の[File]
     */
    private suspend fun copyFileFromUri(uri: Uri, targetFile: File) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)!!
        targetFile.writeBytes(inputStream.readBytes()) // 2GBを超えると使えない
    }

    /**
     * Fileを端末の動画フォルダへコピーする
     *
     * @param context [Context]
     * @param copyFile コピーするファイル
     * @param fileName ファイル名
     * @param mimeType MimeType
     * @param folderName 動画フォルダの中に作るフォルダ名。Android 10以降のみ
     */
    private suspend fun copyDeviceMovieFolder(context: Context, copyFile: File, folderName: String, fileName: String, mimeType: String) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        // MediaStoreに入れる中身
        val contentValues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValuesOf(
                MediaStore.MediaColumns.MIME_TYPE to mimeType,
                MediaStore.MediaColumns.DISPLAY_NAME to fileName,
                MediaStore.MediaColumns.RELATIVE_PATH to resultFileFolder(folderName)
            )
        } else {
            contentValuesOf(MediaStore.MediaColumns.DISPLAY_NAME to fileName)
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)!!
        val outputStream = contentResolver.openOutputStream(uri)!!
        outputStream.write(copyFile.readBytes())
        copyFile.delete()
    }

    companion object {
        /** Uriでは扱えないのでFileにした際のファイル名 */
        private const val READ_VIDEO_URI_NAME = "read_from_video_uri"

        /**
         * 保存先を返す。FileAPIで直接アクセスは出来ません
         *
         * @param folderName フォルダ名
         */
        fun resultFileFolder(folderName: String): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // MediaStore.MediaColumns.RELATIVE_PATH が Android10 以降のみ
            "${Environment.DIRECTORY_MOVIES}/$folderName"
        } else {
            Environment.DIRECTORY_MOVIES
        }
    }
}