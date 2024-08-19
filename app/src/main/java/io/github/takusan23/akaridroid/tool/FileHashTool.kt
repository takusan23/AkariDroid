package io.github.takusan23.akaridroid.tool

import android.content.Context
import androidx.core.net.toUri
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/** ファイルのハッシュ値を出す */
object FileHashTool {

    /**
     * [RenderData.FilePath] の MD5 ハッシュ値をを計算する
     *
     * @param context [Context]
     * @param filePath Uri か File
     */
    suspend fun calcMd5(context: Context, filePath: RenderData.FilePath): String {
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

}