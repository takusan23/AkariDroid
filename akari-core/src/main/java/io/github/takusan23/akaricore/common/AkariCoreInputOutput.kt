package io.github.takusan23.akaricore.common

import android.content.Context
import android.media.MediaDataSource
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import io.github.takusan23.akaricore.common.AkariCoreInputOutput.AndroidUri
import io.github.takusan23.akaricore.common.AkariCoreInputOutput.JavaFile
import io.github.takusan23.akaricore.common.AkariCoreInputOutput.OutputJavaByteArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.InputStream
import java.io.OutputStream

/** [File]から[AkariCoreOutputData]を作る */
fun File.toAkariCoreInputOutputData() = AkariCoreInputOutput.JavaFile(this)

/** [Uri]から[AkariCoreOutputData]を作る */
fun Uri.toAkariCoreInputOutputData(context: Context) = AkariCoreInputOutput.AndroidUri(context, this)

/** [ByteArray]から[AkariCoreOutputData]を作る */
fun ByteArray.toAkariCoreInputOutputData() = AkariCoreInputOutput.InputJavaByteArray(this)

/**
 * akari-core で[Uri]と[File]両方に対応するための一枚噛んでるクラス。
 * [Uri]が取れるときと、[File]が取れるとき、両方あると思うので・・・。
 * Android だと[Uri]便利なんだけど、テストコードとか、アプリ固有ストレージの場合は[File]のが欲しいのよね。
 *
 * [JavaFile]ならほぼ動くはず、[AndroidUri]と[InputJavaByteArray][OutputJavaByteArray]は動かないやつがいる
 *
 */
sealed interface AkariCoreInputOutput {

    sealed interface Input {
        /** ファイルを読み出すための[InputStream]。close するのは呼び出し側で */
        fun inputStream(): InputStream
    }

    sealed interface Output {
        /** ファイルを書き込むための[OutputStream]。close するのは呼び出し側で */
        fun outputStream(): OutputStream
    }

    /**
     * Android の[Uri]を入出力として使う
     * [Uri]はプロセス生きてる間だけ有効なので、[Uri]の永続化が必要なら[android.content.ContentResolver.takePersistableUriPermission]
     *
     * [getReadOnlyFileDescriptor]と[getWritableFileDescriptor]ですが、
     * フォトピッカーとか、StorageAccessFramework 経由で取り出した[Uri]は読み取りしか出来ないのでそのため。
     *
     * @param context [Context]
     * @param uri [Uri]
     */
    class AndroidUri(
        private val context: Context,
        private val uri: Uri
    ) : AkariCoreInputOutput, Input, Output {
        override fun inputStream(): InputStream = context.contentResolver.openInputStream(uri)!!

        override fun outputStream(): OutputStream = context.contentResolver.openOutputStream(uri)!!

        /**
         * [FileDescriptor]が必要なとき用。
         * [MediaExtractorTool]で使う。
         */
        fun getReadOnlyFileDescriptor(): ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")!!

        /**
         * [FileDescriptor]が必要なとき用。
         * [MediaMuxerTool]で使う。
         */
        fun getWritableFileDescriptor(): ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "rw")!!
    }

    /**
     * Java の[File]を入出力として使う
     *
     * @param file [File]。
     */
    class JavaFile(
        private val file: File
    ) : AkariCoreInputOutput, Input, Output {
        /** ファイルパス。[MediaExtractorTool]で使う */
        val filePath: String
            get() = file.path

        override fun inputStream(): InputStream = file.inputStream()

        override fun outputStream(): OutputStream = file.outputStream()
    }

    /**
     * データ型の[ByteArray]を入力として使う
     * TODO [MediaExtractorTool]で使う場合は Android 6 以降が必要です。そもそもあんまり使うべきじゃないかも
     *
     * @param byteArray [ByteArray]
     */
    class InputJavaByteArray(
        val byteArray: ByteArray
    ) : AkariCoreInputOutput, Input {
        override fun inputStream(): InputStream = byteArray.inputStream()

        /** [MediaExtractorTool] で使う */
        val mediaDataSource: MediaDataSource? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /**
             * 多分使うこと無いけど、[MediaExtractorTool]で使えるように一応実装しておいた。
             * frameworks/base/media/java/android/media/MediaDataSource.java
             *
             * Copyright (C) 2012 The Android Open Source Project
             *
             * Licensed under the Apache License, Version 2.0 (the "License");
             * you may not use this file except in compliance with the License.
             * You may obtain a copy of the License at
             *
             *      http://www.apache.org/licenses/LICENSE-2.0
             *
             * Unless required by applicable law or agreed to in writing, software
             * distributed under the License is distributed on an "AS IS" BASIS,
             * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
             * See the License for the specific language governing permissions and
             * limitations under the License.
             */
            object : MediaDataSource() {
                override fun close() {
                    // do nothing
                }

                override fun readAt(position: Long, buffer: ByteArray?, offset: Int, size: Int): Int {
                    var returnSize = size
                    if (position >= byteArray.size) {
                        return -1
                    }
                    if (position + returnSize > byteArray.size) {
                        returnSize -= (position + returnSize - byteArray.size).toInt()
                    }
                    System.arraycopy(byteArray, position.toInt(), buffer, offset, returnSize)
                    return returnSize
                }

                override fun getSize(): Long {
                    return byteArray.size.toLong()
                }
            }
        } else {
            null
        }
    }

    /**
     * データ型の[ByteArray]を出力として使う
     * [outputStream]に書き込んだ結果が[byteArray]で取れます
     * TODO [android.media.MediaMuxer]には使えない
     */
    class OutputJavaByteArray : AkariCoreInputOutput, Output {
        private val byteArrayOutputStream = ByteArrayOutputStream()

        override fun outputStream(): OutputStream = byteArrayOutputStream

        /** [outputStream]に書き込んだ結果を受け取る */
        val byteArray: ByteArray
            get() = byteArrayOutputStream.toByteArray()
    }

}
