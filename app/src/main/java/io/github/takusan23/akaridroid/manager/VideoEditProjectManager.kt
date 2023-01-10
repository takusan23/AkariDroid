package io.github.takusan23.akaridroid.manager

import android.content.Context
import io.github.takusan23.akaridroid.data.AkariProjectData
import io.github.takusan23.akaridroid.tool.JsonTool
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