package io.github.takusan23.akaridroid.tool

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.tool.ProjectFolderManager.CLIPBOARD_TIMELINE_JSON_PATH
import io.github.takusan23.akaridroid.tool.ProjectFolderManager.copyToProjectFolder
import io.github.takusan23.akaridroid.tool.ProjectFolderManager.exportPortableProject
import io.github.takusan23.akaridroid.tool.ProjectFolderManager.readRenderData
import io.github.takusan23.akaridroid.tool.data.ProjectItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
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

    /** タイムラインのコピペ機能の JSON 保存先。List<RenderData.RenderItem> が JSON エンコードされたもの */
    private const val CLIPBOARD_TIMELINE_JSON_PATH = "shared_clipboard.json"

    /** タイムラインをコピーしたときの MIME-Type */
    const val TIMELINE_COPY_MIME_TYPE = "application/vnd.akaridroid.timeline.copy"

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
        if (readRenderData(context, name) != null) return

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
            }
            ?.sortedBy { it.lastModifiedDate }
            ?: emptyList()
    }

    /**
     * [RenderData.RenderItem]をコピー出来るようにする。[CLIPBOARD_TIMELINE_JSON_PATH]に保存され、Uri が返される。
     * この Uri はこのアプリのアプリ内固有ストレージを指している。
     *
     * JSON をそのまま入れると見えてしまうため、ファイルに書き出し Uri のみをもたせる。
     * MIME-Type もそう。
     *
     * @param context [Context]
     * @param renderItemList [RenderData.RenderItem]の配列
     * @return JSON 文字列
     */
    suspend fun renderItemToJson(
        context: Context,
        renderItemList: List<RenderData.RenderItem>
    ): Uri {
        // 共有する Uri は content:// が必要なので、あかりんくの実装を間借りする...
        val (clipboardJsonFile, sharedUri) = AkaLinkTool.createAkaLinkFileUri(context, CLIPBOARD_TIMELINE_JSON_PATH)
        // ファイルに書き出す
        val jsonString = withContext(Dispatchers.Default) {
            jsonSerialization.encodeToString(renderItemList)
        }
        clipboardJsonFile.writeText(jsonString)
        return sharedUri
    }

    /**
     * タイムラインのアイテムをコピーした ClipData の中にある Uri を取り出しタイムラインに追加する。
     *
     * @param context [Context]
     * @param uri クリップボードから JSON を取り出したもの
     * @return [RenderData.RenderItem]の配列
     */
    suspend fun jsonRenderItemToList(
        context: Context,
        uri: Uri
    ): List<RenderData.RenderItem> {
        val jsonString = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)!!.bufferedReader().readText()
        }
        return withContext(Dispatchers.Default) {
            jsonSerialization.decodeFromString<List<RenderData.RenderItem>>(jsonString)
        }
    }

    /**
     * 永続化出来ない Uri のファイルを自分のフォルダにコピーする。
     * 二重にコピーすることになる。が、一部の Uri はアクセス権が揮発してしまう（多分永続化出来ない）ので多分必要。
     *
     * TODO ファイル読み込み権限を追加した場合、ContentProvider で _DATA カラムがあればそれを返し、ない場合のみコピーする
     * TODO Uri はローカル以外（Google フォトのバックアップ済み端末に無い写真など）からも取ってこれるので、その場合はやっぱりコピーが必要
     *
     * @param context [Context]
     * @param name プロジェクト名
     * @param uri ドラッグアンドドロップやクリップボードからのペースト
     * @return ファイルパス
     */
    suspend fun copyToProjectFolder(
        context: Context,
        name: String,
        uri: Uri
    ): String {
        return context.contentResolver.openInputStream(uri)!!.use { inputStream ->
            copyToProjectFolder(
                context = context,
                projectName = name,
                fileName = MediaStoreTool.getFileName(context, uri) ?: System.currentTimeMillis().toString(),
                from = inputStream
            ).path
        }
    }

    /** [copyToProjectFolder]の[File]版。 */
    suspend fun copyToProjectFolder(
        context: Context,
        name: String,
        file: File
    ): String {
        // TODO このアプリの File に限定する、もしストレージ読み込み権限が実装された際には動かないようにする必要あり
        return file.inputStream().use { inputStream ->
            copyToProjectFolder(
                context = context,
                projectName = name,
                fileName = file.name,
                from = inputStream
            ).path
        }
    }

    /**
     * TODO 実験的
     * TODO Android 7 以前のサポート
     * TODO 時間がかかるので snackbar か何かを出す
     * TODO これアプリケーションIDも何もかもが一致している必要がある
     * ポータブルプロジェクトを作る。
     * 持ち出せるよう、端末依存の[RenderData.FilePath.Uri]を全て別のファイルにコピーし[RenderData.FilePath.File]にパスを書き直す。
     * フォルダごと移動させたあと[readRenderData]で出来るはず。
     *
     * @param context [Context]
     * @param name プロジェクト名
     * @param zipUri zip ファイルの保存先。[Uri]
     * @param onUpdateProgress zip 圧縮の進捗。現在の進捗と合計ファイル数。
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun exportPortableProject(
        context: Context,
        name: String,
        zipUri: Uri,
        onUpdateProgress: (current: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val renderData = readRenderData(context, name) ?: return@withContext
        // 保存先のファイル名を出す
        val portableName = UriTool.getFileName(context, zipUri)!!
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

            // 重複を消す
            // ただし、ファイル名が同じだが、ファイルパスが違う場合
            // （例えばエクスポートしたのを取り込んで、同じ名前のファイルを Uri 経由で再度取り込むなど）
            // zip に入れる際はファイル名がユニークである必要があるため、ここで修正する
            val insertFilePathToFixFileNameMap = (audioFileList + videoFileList).distinct().groupBy {
                when (val path = it) {
                    is RenderData.FilePath.File -> File(path.filePath).name
                    is RenderData.FilePath.Uri -> UriTool.getFileName(context, path.uriPath.toUri())!!
                }
            }.map { (fileName, someFileNamePathList) ->
                // 一つだけなら何もしない
                if (someFileNamePathList.size == 1) {
                    listOf(someFileNamePathList.first() to fileName)
                } else {
                    // 複数ある場合はファイル名に _index をつける
                    someFileNamePathList.mapIndexed { index, path ->
                        val fixFileName = "${fileName}_${index + 1}"
                        path to fixFileName
                    }
                }
            }.flatten()

            // 進捗用。+1 は JSON 用
            val allFileListSize = insertFilePathToFixFileNameMap.size + 1
            var progressCount = 0

            /** 進捗を[onUpdateProgress]で報告する */
            fun updateProgress() {
                onUpdateProgress(progressCount++, allFileListSize)
            }

            // zip に入れる
            // もとの FilePath と zip 解凍後のパス
            val zipEntryPathList = insertFilePathToFixFileNameMap.associate { (filePath, fileName) ->
                // 追加
                zipOutputStream.createZipEntryAndCopy(context, fileName, filePath)
                // 進捗を報告
                updateProgress()
                filePath to portableProjectPath.resolve(fileName).pathString
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
            updateProgress()
            zipOutputStream.closeEntry()
        }
    }

    /**
     * ポータブルプロジェクトを取り込む
     * TODO 時間がかかるので snackbar か何かを出す
     *
     * @param zipUri 選択した zip ファイルの[Uri]
     * @param onUpdateProgress zip 解凍の進捗。現在の進捗と合計ファイル数。
     */
    suspend fun importPortableProject(
        context: Context,
        zipUri: Uri,
        onUpdateProgress: (current: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val zipFileName = UriTool.getFileName(context, zipUri)!!
        // zip じゃない場合は何もしない
        if (!zipFileName.endsWith(".zip")) return@withContext

        val projectFolder = getProjectFolder(context, zipFileName.fileNameWithoutExtension)

        // TODO ファイル数を出す。本当は ZipFile#getSize() を使えばいいが、File API だけなので
        // TODO でもこれのせいで余計な時間がかかっている。。。
        onUpdateProgress(0, 0)
        val totalCount = ZipInputStream(context.contentResolver.openInputStream(zipUri)).use { zipInputStream ->
            var count = 0
            while (isActive) {
                val entry = zipInputStream.nextEntry ?: break
                count++
                zipInputStream.closeEntry()
            }
            count
        }

        // 進捗用。+1 は JSON 用
        val allFileListSize = totalCount
        var progressCount = 0

        /** 進捗を[onUpdateProgress]で報告する */
        fun updateProgress() {
            onUpdateProgress(progressCount++, allFileListSize)
        }

        // 解凍していく
        ZipInputStream(context.contentResolver.openInputStream(zipUri)).use { zipInputStream ->
            while (isActive) {
                // もうない場合
                val zipEntry = zipInputStream.nextEntry ?: break
                // ファイルを作り取り出す
                val copyFile = projectFolder.resolve(zipEntry.name).apply { createNewFile() }
                copyFile.outputStream().use { outputStream ->
                    zipInputStream.copyTo(outputStream)
                }
                updateProgress()
            }
        }
    }

    /**
     * 自分のフォルダにコピーする。
     * ファイル名が重複していれば (1) を付ける
     *
     * @param context [Context]
     * @param projectName プロジェクト名
     * @param fileName コピーするファイルの名前
     * @param from [InputStream]
     * @return 作ったファイル
     */
    private suspend fun copyToProjectFolder(
        context: Context,
        projectName: String,
        fileName: String,
        from: InputStream
    ): File {
        return withContext(Dispatchers.IO) {
            // TODO このアプリの File に限定する、もしストレージ読み込み権限が実装された際には動かないようにする必要あり
            val folder = getProjectFolder(context, projectName)

            // すでにあれば (2) みたいにする
            val uniqueFileName = if (folder.resolve(fileName).exists()) {
                // すでにある。早期的に探す
                val (withoutExtensionFileName, extension) = fileName.split(".")
                var count = 1
                var maybeFileName: String
                while (true) {
                    // 作って exists が false なら break
                    maybeFileName = "$withoutExtensionFileName($count).$extension"
                    count++
                    if (!folder.resolve(maybeFileName).exists()) {
                        break
                    }
                }
                maybeFileName
            } else {
                // ない
                fileName
            }

            // InputStream を開いてコピー
            val file = folder.resolve(uniqueFileName)
            file.outputStream().use { outputStream ->
                from.copyTo(outputStream)
            }

            file
        }
    }

    /**
     * [ZipOutputStream]へ[RenderData.FilePath]を追加する
     *
     * @param context [Context]
     * @param fileName [filePath]のファイル名がかぶる場合があるので、変更できるように
     * @param filePath 追加する [RenderData.FilePath]
     */
    private suspend fun ZipOutputStream.createZipEntryAndCopy(
        context: Context,
        fileName: String,
        filePath: RenderData.FilePath
    ) = withContext(Dispatchers.IO) {
        when (filePath) {
            is RenderData.FilePath.File -> {
                val file = File(filePath.filePath)
                putNextEntry(ZipEntry(fileName))
                file.inputStream().buffered().use { inputStream ->
                    inputStream.copyTo(this@createZipEntryAndCopy)
                }
                closeEntry()
            }

            is RenderData.FilePath.Uri -> {
                val uri = filePath.uriPath.toUri()
                putNextEntry(ZipEntry(fileName))
                context.contentResolver.openInputStream(uri)!!.buffered().use { inputStream ->
                    inputStream.copyTo(this@createZipEntryAndCopy)
                }
                closeEntry()
            }
        }
    }

    /** 拡張子を除く */
    private val String.fileNameWithoutExtension
        get() = this.split(".").first()

}