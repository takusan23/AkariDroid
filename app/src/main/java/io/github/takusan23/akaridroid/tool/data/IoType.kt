package io.github.takusan23.akaridroid.tool.data

import android.net.Uri
import androidx.core.net.toUri
import io.github.takusan23.akaridroid.RenderData
import java.io.File

/** [IoType.AndroidUri]を作る拡張関数 */
fun Uri.toIoType() = IoType.AndroidUri(this)

/** [IoType.JavaFile]を作る拡張関数 */
fun File.toIoType() = IoType.JavaFile(this)

/** [RenderData.FilePath]と相互変換 */
fun RenderData.FilePath.toIoType() = when (this) {
    is RenderData.FilePath.File -> File(this.filePath).toIoType()
    is RenderData.FilePath.Uri -> this.uriPath.toUri().toIoType()
}

/** [RenderData.FilePath]と相互変換 */
fun IoType.toRenderDataFilePath() = when (this) {
    is IoType.AndroidUri -> RenderData.FilePath.Uri(this.uri.toString())
    is IoType.JavaFile -> RenderData.FilePath.File(this.file.path)
}

/**
 * ファイルパスを表す
 * Android の Uri と、Java の File をいい感じに sealed class で表現する。
 *
 * [RenderData.FilePath]とか言う似たようなのがあるけど、こっちはシリアライズしないので。
 */
sealed interface IoType {


    /**
     * Android の Uri から [IoType] を作る
     * Storage Access Framework とかフォトピッカーだとこれ
     */
    data class AndroidUri(val uri: Uri) : IoType

    /**
     * Java の File から [IoType] を作る
     * Context#getExternalFilesDir はこっち
     */
    data class JavaFile(val file: File) : IoType

}