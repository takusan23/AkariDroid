package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.takusan23.akaridroid.data.AkariProjectData
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.data.CanvasElementType
import io.github.takusan23.akaridroid.data.VideoOutputFormat
import io.github.takusan23.akaridroid.manager.VideoEditProjectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoEditorViewModel(application: Application, private val projectId: String) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val videoEditProjectManager by lazy { VideoEditProjectManager(context) }

    private var akariProjectData: AkariProjectData? = null
    private val _canvasElementList = MutableStateFlow<List<CanvasElementData>>(listOf())
    private val _videoFilePath = MutableStateFlow<String?>(null)
    private val _videoOutputFormat = MutableStateFlow(VideoOutputFormat())

    /** Canvasに描画する要素 */
    val canvasElementList = _canvasElementList.asStateFlow()

    /** 動画パス */
    val videoFilePath = _videoFilePath.asStateFlow()

    /** エンコードする際の動画情報（フレームレートとか） */
    val videoOutputFormat = _videoOutputFormat.asStateFlow()

    init {
/*
        _canvasElementList.value = listOf("文字を動画の上に書く", "Hello World", "あたまいたい")
            .mapIndexed { index, text ->
                CanvasElementData(
                    id = index.toLong(),
                    xPos = 100f,
                    yPos = 100f * (index + 1),
                    startMs = 0,
                    endMs = 100,
                    elementType = CanvasElementType.TextElement(text, Color.RED, 80f)
                )
            }
*/
        // ロードする
        viewModelScope.launch {
            // まだプロジェクト作成機能がないので、とりあえずハードコートしたファイルパスでロードする
            // 初回時は絶対落ちるので runCatching
            val loadAkariProjectData = runCatching {
                videoEditProjectManager.loadProjectData(projectId)
            }.getOrNull() ?: AkariProjectData()

            akariProjectData = loadAkariProjectData
            _canvasElementList.value = loadAkariProjectData.canvasElementList
            _videoFilePath.value = loadAkariProjectData.videoFilePath
            _videoOutputFormat.value = loadAkariProjectData.videoOutputFormat
        }
    }

    /**
     * 要素を更新する
     *
     * @param after [CanvasElementData]
     */
    fun updateElement(after: CanvasElementData) {
        _canvasElementList.value = canvasElementList.value.map { before ->
            if (before.id == after.id) {
                after.copy()
            } else before
        }
    }

    /**
     * 要素を削除する
     *
     * @param delete 削除する要素
     */
    fun deleteElement(delete: CanvasElementData) {
        _canvasElementList.value = canvasElementList.value.filter { it.id != delete.id }
    }

    /**
     * 動画ファイルをセットする。Uriをプロジェクトフォルダへコピーする。
     *
     * @param uri ファイルピッカーで選んだUri
     */
    fun setVideoFile(uri: Uri?) {
        uri ?: return
        viewModelScope.launch {
            _videoFilePath.value = null
            val videoFile = videoEditProjectManager.addFileToProject(projectId, uri, "videofile")
            _videoFilePath.value = videoFile.path
        }
    }

    /**
     * テキスト要素を作成する
     *
     * @return 新規作成された [CanvasElementData]
     */
    fun addTextElement(): CanvasElementData {
        val canvasElementData = CanvasElementData(
            xPos = 100f,
            yPos = 100f,
            startMs = 0,
            endMs = 100,
            elementType = CanvasElementType.TextElement(
                text = "",
                color = Color.WHITE,
                fontSize = 100f
            )
        )
        _canvasElementList.value = canvasElementList.value + canvasElementData
        return canvasElementData
    }

    /** エンコーダーに渡すためのデータを作成して保存する */
    suspend fun saveEncodeData(): AkariProjectData {
        // 保存する。
        val newProjectData = (akariProjectData ?: AkariProjectData()).copy(
            projectId = projectId,
            canvasElementList = canvasElementList.value,
            videoFilePath = videoFilePath.value,
            videoOutputFormat = videoOutputFormat.value
        )
        videoEditProjectManager.saveProjectData(newProjectData)
        return newProjectData
    }

    companion object {

        val PROJECT_ID = object : CreationExtras.Key<String> {}

        val Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                val projectId = this[PROJECT_ID]!!
                VideoEditorViewModel(application, projectId)
            }
        }

    }

}