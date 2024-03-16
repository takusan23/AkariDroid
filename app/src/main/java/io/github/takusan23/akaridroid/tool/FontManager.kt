package io.github.takusan23.akaridroid.tool

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 自分の好きなフォントを追加できる */
class FontManager(private val context: Context) {

    private val fontFolder = context.getExternalFilesDir(null)!!.resolve(CUSTOM_FONT_FOLDER_NAME).apply { mkdir() }

    /** フォントを追加する */
    suspend fun addFont(uri: Uri) {
        val (fileName, mimeType) = getFileNameAndMimeType(uri) ?: return

        // フォントか見る
        if (!mimeType.startsWith("font/")) {
            return
        }

        // 保存する
        withContext(Dispatchers.IO) {
            fontFolder.resolve(fileName).outputStream().use { outputStream ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    /**
     * 自分が追加したフォント一覧を返す
     *
     * @return フォントの配列
     */
    suspend fun getFontList(): List<File> = withContext(Dispatchers.IO) {
        fontFolder.listFiles()?.filter { it.extension == "ttf" } ?: emptyList()
    }

    /**
     * フォントのファイル名から、[Typeface]を作成する
     *
     * @return [Typeface]、フォントが見つからなかった場合は null
     */
    suspend fun createTypeface(fontName: String): Typeface? = withContext(Dispatchers.IO) {
        val fontFile = fontFolder.resolve(fontName)
        if (fontFile.exists()) {
            Typeface.createFromFile(fontFile)
        } else {
            null
        }
    }

    /** 名前と MIME-Type を返す */
    private suspend fun getFileNameAndMimeType(uri: Uri): Pair<String, String>? = withContext(Dispatchers.IO) {
        context.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
            null,
            null,
            null
        )?.use { cursor ->
            cursor.moveToFirst()
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
            val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
            name to mimeType
        }
    }

    companion object {
        private const val CUSTOM_FONT_FOLDER_NAME = "font_folder"
    }
}