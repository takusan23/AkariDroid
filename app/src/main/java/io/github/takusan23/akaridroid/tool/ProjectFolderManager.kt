package io.github.takusan23.akaridroid.tool

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.tool.data.ProjectItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.pathString

/**
 * プロジェクト関連
 * プロジェクト名がフォルダ名になる
 */
object ProjectFolderManager {

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
     * @param name プロジェクト名
     */
    fun getProjectFolder(
        context: Context,
        name: String
    ): File = context.getExternalFilesDir(null)!!.resolve(name).apply { mkdir() }

    /**
     * [RenderData]を読み出す
     *
     * @param context [Context]
     * @return [RenderData]。初回起動時等で存在しない場合は null
     */
    suspend fun readRenderData(
        context: Context,
        name: String
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
        name: String
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
     * プロジェクトを作成する。
     * [RenderData]を作成する。
     */
    suspend fun createProject(context: Context, name: String) {
        // 重複チェック TODO 重複していればエラー
        if (getProjectFolder(context, name).exists()) return

        val defaultRenderData = RenderData()
        writeRenderData(context, defaultRenderData, name)
    }

    /** プロジェクトを削除する */
    suspend fun deleteProject(context: Context, name: String) {
        // Uri へのアクセスを破棄する。takePermission は上限があるので
        val renderData = readRenderData(context, name)
        renderData?.audioRenderItem?.mapNotNull {
            when (it) {
                is RenderData.AudioItem.Audio -> (it.filePath as? RenderData.FilePath.Uri)?.uriPath?.toUri()
            }
        }?.forEach { uri -> UriTool.revokePersistableUriPermission(context, uri) }
        renderData?.canvasRenderItem?.mapNotNull {
            when (it) {
                is RenderData.CanvasItem.Effect,
                is RenderData.CanvasItem.Shader,
                is RenderData.CanvasItem.Shape,
                is RenderData.CanvasItem.SwitchAnimation,
                is RenderData.CanvasItem.Text -> null

                is RenderData.CanvasItem.Image -> (it.filePath as? RenderData.FilePath.Uri)?.uriPath?.toUri()
                is RenderData.CanvasItem.Video -> (it.filePath as? RenderData.FilePath.Uri)?.uriPath?.toUri()
            }
        }?.forEach { uri -> UriTool.revokePersistableUriPermission(context, uri) }

        // 再帰的に消す
        val projectFolder = getProjectFolder(context, name)
        projectFolder.deleteRecursively()
    }

    /**
     * プロジェクト一覧を取得する
     *
     * @param context [Context]
     * @return [ProjectItem]配列
     */
    suspend fun loadProjectList(context: Context): List<ProjectItem> = withContext(Dispatchers.IO) {
        // フォルダ内に render_data.json があれば
        return@withContext context.getExternalFilesDir(null)
            ?.listFiles()
            ?.mapNotNull { file ->
                val projectName = file.name
                val renderData = readRenderData(context, projectName) ?: return@mapNotNull null
                ProjectItem(
                    projectName = projectName,
                    lastModifiedDate = file.lastModified(),
                    videoDurationMs = renderData.durationMs
                )
            } ?: emptyList()
    }

    /**
     * TODO 実験的
     * TODO Android 7 以前のサポート
     * TODO 時間がかかるので snackbar か何かを出す
     * ポータブルプロジェクトを作る。
     * 持ち出せるよう、端末依存の[RenderData.FilePath.Uri]を全て別のファイルにコピーし[RenderData.FilePath.File]にパスを書き直す。
     * フォルダごと移動させたあと[readRenderData]で出来るはず。
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportPortableProject(
        context: Context,
        name: String,
        portableName: String,
        zipUri: Uri
    ) = withContext(Dispatchers.IO) {
        val renderData = readRenderData(context, name) ?: return@withContext
        // 拡張子 zip を消す
        val portableNameWithoutExtension = portableName.fileNameWithoutExtension
        val portableProjectPath = context.getExternalFilesDir(null)!!.resolve(portableNameWithoutExtension).toPath()

        // 保存先 zip に書き込む
        ZipOutputStream(context.contentResolver.openOutputStream(zipUri)).use { zipOutputStream ->

            // zip にいれる。FilePath がキーで展開後のパスが入った Map
            // 音声と動画素材で一緒に（音声と映像で同じ Uri になるので弾く必要がある）
            val audioFileList = renderData.audioRenderItem.map { audioItem ->
                when (audioItem) {
                    is RenderData.AudioItem.Audio -> audioItem.filePath
                }
            }
            val videoFileList = renderData.canvasRenderItem.mapNotNull { canvasItem ->
                when (canvasItem) {
                    is RenderData.CanvasItem.Effect,
                    is RenderData.CanvasItem.Shader,
                    is RenderData.CanvasItem.Shape,
                    is RenderData.CanvasItem.SwitchAnimation,
                    is RenderData.CanvasItem.Text -> null

                    is RenderData.CanvasItem.Image -> canvasItem.filePath
                    is RenderData.CanvasItem.Video -> canvasItem.filePath
                }
            }
            val allFileList = (audioFileList + videoFileList).distinct()

            // zip に入れる
            // もとの FilePath と zip 解凍後のパス
            val zipEntryPathList = allFileList.associateWith { filePath ->
                // 展開後のパスを想定する
                val fileName = zipOutputStream.createZipEntryAndCopy(context, filePath)
                portableProjectPath.resolve(fileName).pathString
            }

            // パスを zip 展開後に置き換え
            val portableRenderData = renderData.copy(
                audioRenderItem = renderData.audioRenderItem.map { audioItem ->
                    when (audioItem) {
                        is RenderData.AudioItem.Audio -> audioItem.copy(filePath = RenderData.FilePath.File(zipEntryPathList[audioItem.filePath]!!))
                    }
                },
                canvasRenderItem = renderData.canvasRenderItem.map { canvasItem ->
                    when (canvasItem) {
                        is RenderData.CanvasItem.Effect,
                        is RenderData.CanvasItem.Shader,
                        is RenderData.CanvasItem.Shape,
                        is RenderData.CanvasItem.SwitchAnimation,
                        is RenderData.CanvasItem.Text -> canvasItem

                        is RenderData.CanvasItem.Image -> canvasItem.copy(filePath = RenderData.FilePath.File(zipEntryPathList[canvasItem.filePath]!!))
                        is RenderData.CanvasItem.Video -> canvasItem.copy(filePath = RenderData.FilePath.File(zipEntryPathList[canvasItem.filePath]!!))
                    }
                }
            )

            // RenderData も zip にいれる
            val jsonString = jsonSerialization.encodeToString(portableRenderData)
            zipOutputStream.putNextEntry(ZipEntry(RENDER_DATA_JSON_FILE_NAME))
            zipOutputStream.write(jsonString.toByteArray())
            zipOutputStream.closeEntry()
        }
    }

    /**
     * ポータブルプロジェクトを取り込む
     * TODO 時間がかかるので snackbar か何かを出す
     *
     * @param zipUri 選択した zip ファイルの[Uri]
     */
    suspend fun importPortableProject(context: Context, zipUri: Uri) = withContext(Dispatchers.IO) {
        val zipFileName = UriTool.getFileName(context, zipUri)!!
        // zip じゃない場合は何もしない
        if (!zipFileName.endsWith(".zip")) return@withContext

        val projectFolder = getProjectFolder(context, zipFileName.fileNameWithoutExtension)
        // 解凍していく
        ZipInputStream(context.contentResolver.openInputStream(zipUri)).use { zipInputStream ->
            // 展開する
            var currentZipEntry: ZipEntry?
            while (isActive) {
                // もうない場合
                currentZipEntry = zipInputStream.nextEntry
                if (currentZipEntry == null) break
                // ファイルを作り取り出す
                val copyFile = projectFolder.resolve(currentZipEntry.name).apply { createNewFile() }
                copyFile.outputStream().use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }
            }
        }
    }

    /**
     * [ZipOutputStream]へ[RenderData.FilePath]を追加する
     *
     * @param context [Context]
     * @param filePath 追加する [RenderData.FilePath]
     * @return ファイル名
     */
    private suspend fun ZipOutputStream.createZipEntryAndCopy(
        context: Context,
        filePath: RenderData.FilePath
    ): String = withContext(Dispatchers.IO) {
        when (filePath) {
            is RenderData.FilePath.File -> {
                val file = File(filePath.filePath)
                val fileName = file.name
                putNextEntry(ZipEntry(fileName))
                file.inputStream().buffered().use { inputStream ->
                    inputStream.copyTo(this@createZipEntryAndCopy)
                }
                closeEntry()
                fileName
            }

            is RenderData.FilePath.Uri -> {
                val uri = filePath.uriPath.toUri()
                val fileName = UriTool.getFileName(context, uri)!!
                putNextEntry(ZipEntry(fileName))
                context.contentResolver.openInputStream(uri)!!.buffered().use { inputStream ->
                    inputStream.copyTo(this@createZipEntryAndCopy)
                }
                closeEntry()
                fileName
            }
        }
    }

    /** 拡張子を除く */
    private val String.fileNameWithoutExtension
        get() = this.split(".").first()

}