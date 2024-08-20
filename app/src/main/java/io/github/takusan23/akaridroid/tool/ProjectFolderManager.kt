package io.github.takusan23.akaridroid.tool

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import io.github.takusan23.akaridroid.RenderData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ProjectFolderManager {

    /** プロジェクト保存先、複数プロジェクトが出来るようになればこの辺も分ける */
    private const val PROJECT_FOLDER_NAME = "akaridroid_project_20240216"

    /** [RenderData]を JSON にしたときのファイル名 */
    private const val RENDER_DATA_JSON_FILE_NAME = "render_data.json"

    /** [exportPortableProject]のフォルダ名 */
    private const val EXPORT_PORTABLE_PROJECT_FOLDER_NAME = "akaridroid_portable_project_20240821"

    /** JSONパースするときに使う */
    private val jsonSerialization = Json {
        // JSONのキーが全部揃ってなくてもパース
        ignoreUnknownKeys = true
        // data class の省略時の値を使うように
        encodeDefaults = true
    }

    /**
     * プロジェクトで利用できるフォルダを返す。
     * デコードした音声とか、エンコード中の一次保存先のとかに使われる。
     *
     * @param context [Context]
     */
    fun getProjectFolder(
        context: Context,
        name: String = PROJECT_FOLDER_NAME
    ): File = context.getExternalFilesDir(null)!!.resolve(name).apply { mkdir() }

    /**
     * [RenderData]を読み出す
     *
     * @param context [Context]
     * @return [RenderData]。初回起動時等で存在しない場合は null
     */
    suspend fun readRenderData(
        context: Context,
        name: String = PROJECT_FOLDER_NAME
    ): RenderData? {
        // ないなら null
        val jsonFile = getProjectFolder(context, name).resolve(RENDER_DATA_JSON_FILE_NAME)
        if (!jsonFile.exists()) return null

        val renderDataJson = withContext(Dispatchers.IO) {
            getProjectFolder(context, name).resolve(RENDER_DATA_JSON_FILE_NAME).readText()
        }
        val renderData = withContext(Dispatchers.Default) {
            jsonSerialization.decodeFromString<RenderData>(renderDataJson)
        }
        return renderData
    }

    /**
     * [RenderData] を JSON にして保存する
     *
     * @param context [Context]
     * @param renderData [RenderData]
     */
    suspend fun writeRenderData(
        context: Context,
        renderData: RenderData,
        name: String = PROJECT_FOLDER_NAME
    ) {
        val jsonFile = getProjectFolder(context, name).resolve(RENDER_DATA_JSON_FILE_NAME)

        val jsonString = withContext(Dispatchers.Default) {
            jsonSerialization.encodeToString(renderData)
        }
        withContext(Dispatchers.IO) {
            jsonFile.writeText(jsonString)
        }
    }

    /**
     * TODO 実験的、他の端末へ持ち運べる何かをちゃんと作る
     * ポータブルプロジェクトを作る。
     * 持ち出せるよう、端末依存の[RenderData.FilePath.Uri]を全て別のファイルにコピーし[RenderData.FilePath.File]にパスを書き直す。
     * フォルダごと移動させたあと[readRenderData]で出来るはず。
     */
    suspend fun exportPortableProject(
        context: Context,
        name: String = PROJECT_FOLDER_NAME
    ) = withContext(Dispatchers.IO) {
        val renderData = readRenderData(context, name) ?: return@withContext
        val exportFolder = context.getExternalFilesDir(null)!!.resolve(EXPORT_PORTABLE_PROJECT_FOLDER_NAME).apply { mkdir() }
        val jsonFile = exportFolder.resolve(RENDER_DATA_JSON_FILE_NAME)

        // 並列でする
        val deferredUpdatedAudioRenderItem = renderData.audioRenderItem.map { audioItem ->
            async {
                when (audioItem) {
                    is RenderData.AudioItem.Audio -> {
                        // Uri なら指定フォルダにコピーする
                        if (audioItem.filePath is RenderData.FilePath.Uri) {
                            val copyFile = copyUriToFile(context, audioItem.filePath.uriPath.toUri(), exportFolder)
                            audioItem.copy(filePath = RenderData.FilePath.File(copyFile.path))
                        } else {
                            audioItem
                        }
                    }
                }
            }
        }
        val deferredUpdatedCanvasRenderItem = renderData.canvasRenderItem.map { canvasItem ->
            async {
                when (canvasItem) {
                    is RenderData.CanvasItem.Effect,
                    is RenderData.CanvasItem.Shader,
                    is RenderData.CanvasItem.Shape,
                    is RenderData.CanvasItem.SwitchAnimation,
                    is RenderData.CanvasItem.Text -> canvasItem

                    is RenderData.CanvasItem.Image -> {
                        if (canvasItem.filePath is RenderData.FilePath.Uri) {
                            val copyFile = copyUriToFile(context, canvasItem.filePath.uriPath.toUri(), exportFolder)
                            canvasItem.copy(filePath = RenderData.FilePath.File(copyFile.path))
                        } else {
                            canvasItem
                        }
                    }

                    is RenderData.CanvasItem.Video -> {
                        if (canvasItem.filePath is RenderData.FilePath.Uri) {
                            val copyFile = copyUriToFile(context, canvasItem.filePath.uriPath.toUri(), exportFolder)
                            canvasItem.copy(filePath = RenderData.FilePath.File(copyFile.path))
                        } else {
                            canvasItem
                        }
                    }
                }
            }
        }
        // 待ち合わせして更新
        val portableRenderData = renderData.copy(
            audioRenderItem = deferredUpdatedAudioRenderItem.awaitAll(),
            canvasRenderItem = deferredUpdatedCanvasRenderItem.awaitAll()
        )
        val jsonString = jsonSerialization.encodeToString(portableRenderData)
        jsonFile.writeText(jsonString)
    }

    /** Uri をコピーする TODO UriTool にいるべき */
    private suspend fun copyUriToFile(context: Context, uri: Uri, parentFolder: File): File = withContext(Dispatchers.IO) {
        val name = UriTool.getFileName(context, uri) ?: System.currentTimeMillis().toString()
        val copyFile = parentFolder.resolve(name)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            copyFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return@withContext copyFile
    }

}