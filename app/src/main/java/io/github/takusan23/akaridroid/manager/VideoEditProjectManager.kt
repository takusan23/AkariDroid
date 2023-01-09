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
        val projectFolder = File(videoEditFolder, akariProjectData.projectId).apply {
            if (!exists()) {
                mkdir()
            }
        }
        // JSON で保存
        File(projectFolder, PROJECT_JSON_FILE_NAME).apply {
            val jsonText = JsonTool.encode(akariProjectData)
            writeText(jsonText)
        }
    }

    /**
     * プロジェクトを読み込む
     *
     * @return [AkariProjectData]
     */
    suspend fun loadProjectData(projectId: String) = withContext(Dispatchers.IO) {
        val projectFolder = File(videoEditFolder, projectId).apply {
            if (!exists()) {
                mkdir()
            }
        }
        val jsonText = File(projectFolder, PROJECT_JSON_FILE_NAME).readText()
        return@withContext JsonTool.parse<AkariProjectData>(jsonText)
    }

    companion object {

        /** プロジェクトJSONファイルの名前 */
        private const val PROJECT_JSON_FILE_NAME = "project.json"
    }

}