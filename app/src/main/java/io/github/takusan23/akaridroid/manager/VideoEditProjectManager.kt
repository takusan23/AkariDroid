package io.github.takusan23.akaridroid.manager

import android.content.Context
import android.net.Uri
import io.github.takusan23.akaridroid.data.AkariProjectData
import io.github.takusan23.akaridroid.tool.JsonTool
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * プロジェクトを管理する
 *
 * @param context [Context]
 */
class VideoEditProjectManager(private val context: Context) {

    /** 保存先 */
    private val videoEditFolder by lazy {
        File(context.getExternalFilesDir(null), "project").apply {
            if (!exists()) {
                mkdir()
            }
        }
    }

    /**
     * プロジェクトを保存する
     *
     * @param akariProjectData
     */
    suspend fun saveProjectData(akariProjectData: AkariProjectData) = withContext(Dispatchers.IO) {
        val projectFolder = getProjectFolder(akariProjectData.projectId)
        // JSON で保存
        val jsonText = JsonTool.encode(akariProjectData)
        File(projectFolder, PROJECT_JSON_FILE_NAME).writeText(jsonText)
    }

    /**
     * プロジェクトを読み込む
     *
     * @return [AkariProjectData]
     */
    suspend fun loadProjectData(projectId: String): AkariProjectData = withContext(Dispatchers.IO) {
        val projectFolder = getProjectFolder(projectId)
        // JSON から データクラスへ
        val jsonText = File(projectFolder, PROJECT_JSON_FILE_NAME).readText()
        return@withContext JsonTool.parse<AkariProjectData>(jsonText)
    }

    /**
     * プロジェクトにファイルを追加する
     *
     * @param projectId プロジェクトID
     * @param uri Uri
     * @return [File]
     */
    suspend fun addFileToProject(projectId: String, uri: Uri, fileName: String) = withContext(Dispatchers.IO) {
        val projectFolder = getProjectFolder(projectId)
        val addFile = File(projectFolder, fileName).apply {
            createNewFile()
        }
        MediaStoreTool.fileCopy(context, uri, addFile)
        return@withContext addFile
    }

    /**
     * プロジェクトのファイルを削除する
     *
     * @param filePath ファイルパス
     */
    suspend fun deleteFile(filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).delete()
    }

    /**
     * プロジェクトのフォルダの[File]を取得する
     *
     * @param projectId プロジェクトID
     * @return [File]
     */
    private suspend fun getProjectFolder(projectId: String): File = withContext(Dispatchers.IO) {
        return@withContext File(videoEditFolder, projectId).apply {
            if (!exists()) {
                mkdir()
            }
        }
    }

    companion object {

        /** プロジェクトJSONファイルの名前 */
        private const val PROJECT_JSON_FILE_NAME = "project.json"
    }

}