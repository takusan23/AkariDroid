package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.takusan23.akaridroid.data.AkariProjectData
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.manager.VideoEditProjectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoEditorViewModel(application: Application, private val projectId: String) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val videoEditProjectManager by lazy { VideoEditProjectManager(context) }

    private val _canvasElementList = MutableStateFlow<List<CanvasElementData>>(listOf())

    val canvasElementList = _canvasElementList.asStateFlow()

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
            val akariProjectData = videoEditProjectManager.loadProjectData("project-2022-01-10")
            _canvasElementList.value = akariProjectData.canvasElementList
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
     * エンコーダーに渡すためのデータを作成して保存する
     * 多分 Bundle に入れると多分サイズオーバーしてしまうので、一旦ストレージに保存する
     */
    suspend fun saveEncodeData(): AkariProjectData {
        // 保存する
        val akariProjectData = AkariProjectData(
            canvasElementList = canvasElementList.value
        )
        videoEditProjectManager.saveProjectData(akariProjectData)
        return akariProjectData
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