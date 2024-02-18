package io.github.takusan23.akaricore.v2.common

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileDescriptor
import java.io.InputStream

/** [File]から[AkariCoreInputDataSource]を作る */
fun File.toAkariCoreInputDataSource() = AkariCoreInputDataSource.JavaFile(this)

/** [Uri]から[AkariCoreInputDataSource]を作る */
fun Uri.toAkariCoreInputDataSource(context: Context) = AkariCoreInputDataSource.AndroidUri(context, this)

/**
 * akari-core で[Uri]と[File]両方に対応するための一枚噛んでるクラス。
 * [Uri]が取れるときと、[File]が取れるとき、両方あると思うので・・・
 *
 * Android だと[Uri]便利なんだけど、テストコードとか、アプリ固有ストレージの場合は[File]のが欲しいのよね。
 */
sealed interface AkariCoreInputDataSource {

    /** ファイルを読み出すための InputStream。close するのは呼び出し側で */
    fun getInputStream(): InputStream

    /**
     * [FileDescriptor]が必要なとき用。
     * [MediaExtractorTool]で使う。
     *
     * Android の[android.content.ContentResolver.openFileDescriptor]に合わせるために[FileDescriptor]ではなくこれになっている。
     * これも close は呼び出し側で
     */
    fun getFileDescriptor(): ParcelFileDescriptor

    /**
     * Android の[Uri]を入力として使う
     * [Uri]はプロセス生きてる間だけ有効なので、[Uri]の永続化が必要なら[android.content.ContentResolver.takePersistableUriPermission]
     *
     * @param context [Context]
     * @param uri [Uri]
     */
    class AndroidUri(
        private val context: Context,
        private val uri: Uri // public にしても使い道ないので private
    ) : AkariCoreInputDataSource {
        override fun getInputStream(): InputStream = context.contentResolver.openInputStream(uri)!!

        override fun getFileDescriptor(): ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")!!
    }

    /**
     * Java の[File]を入力として使う
     *
     * @param file [File]
     */
    class JavaFile(val file: File) : AkariCoreInputDataSource {
        override fun getInputStream(): InputStream = file.inputStream()

        override fun getFileDescriptor(): ParcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

}
