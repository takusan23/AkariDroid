package io.github.takusan23.akaridroid.tool

import android.content.Context
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * ファイル読み書きを Koin DI 経由で提供する
 * getExternalFilesDir みたいな
 *
 * Koin DI ライブラリ経由でこのクラスのインスタンスが取得できます
 *
 * @param context Koin 経由で
 */
class FileTool(private val context: Context) {

    /** [Context.getExternalFilesDir] を呼び出す */
    fun getExternalFilesDir() = context.getExternalFilesDir(null)!!

    /**
     * [RenderData.FilePath] の MD5 ハッシュ値をを計算する
     *
     * @param filePath Uri か File
     */
    suspend fun calcMd5(filePath: RenderData.FilePath): String {
        return when (filePath) {
            is RenderData.FilePath.File -> File(filePath.filePath).inputStream().buffered()
            is RenderData.FilePath.Uri -> context.contentResolver.openInputStream(filePath.uriPath.toUri())
        }!!.use { calcMd5(it) }
    }

    /**
     * ファイルの MD5 ハッシュ値を出す
     *
     * @param inputStream ファイルの InputStream
     * @return MD5 文字列
     */
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun calcMd5(inputStream: InputStream): String = withContext(Dispatchers.IO) {
        DigestInputStream(inputStream, MessageDigest.getInstance("md5")).use { digestInputStream ->
            val byteArray = ByteArray(8 * 1024) // InputStream#copyTo と同じ
            while (isActive) {
                val size = digestInputStream.read(byteArray)
                if (size == -1) break
            }
            digestInputStream.messageDigest.digest().toHexString()
        }
    }

    /**
     * 画像をロードする
     *
     * @param filePath Uri か ファイルパスか
     * @param width よこはば
     * @param height たてはば
     */
    suspend fun getBitmap(
        filePath: RenderData.FilePath,
        width: Int,
        height: Int
    ) = withContext(Dispatchers.IO) {
        Glide
            .with(context)
            .asBitmap()
            .load(
                when (filePath) {
                    is RenderData.FilePath.File -> filePath.filePath
                    is RenderData.FilePath.Uri -> filePath.uriPath
                }
            )
            .submit(width, height)
            .get()
    }

}