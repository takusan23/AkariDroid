package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaridroid.data.*
import io.github.takusan23.akaridroid.manager.VideoEditProjectManager
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 編集画面のViewModel
 *
 * @param savedStateHandle JetpackCompose Navigation のクエリパラメーターが取得できる
 */
class VideoEditorViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val videoEditProjectManager by lazy { VideoEditProjectManager(context) }

    private var akariProjectData: AkariProjectData? = null
    private val _canvasElementList = MutableStateFlow<List<CanvasElementData>>(emptyList())
    private val _videoFileData = MutableStateFlow<VideoFileData?>(null)
    private val _videoOutputFormat = MutableStateFlow(VideoOutputFormat())
    private val _audioAssetList = MutableStateFlow<List<AudioAssetData>>(emptyList())

    /** Canvasに描画する要素 */
    val canvasElementList = _canvasElementList.asStateFlow()

    /** 動画パス */
    val videoFileData = _videoFileData.asStateFlow()

    /** エンコードする際の動画情報（フレームレートとか） */
    val videoOutputFormat = _videoOutputFormat.asStateFlow()

    /** 音声データ */
    val audioAssetList = _audioAssetList.asStateFlow()

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
                videoEditProjectManager.loadProjectData(savedStateHandle["project_id"]!!)
            }.getOrNull() ?: AkariProjectData()

            akariProjectData = loadAkariProjectData
            _canvasElementList.value = loadAkariProjectData.canvasElementList
            _videoFileData.value = loadAkariProjectData.videoFileData
            _videoOutputFormat.value = loadAkariProjectData.videoOutputFormat
            _audioAssetList.value = loadAkariProjectData.audioAssetList
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
     * 音声要素を更新する
     *
     * @param after [AudioAssetData]
     */
    fun updateAudioAssetData(after: AudioAssetData) {
        _audioAssetList.value = audioAssetList.value.map { before ->
            if (before.id == after.id) {
                after.copy()
            } else before
        }
    }

    /**
     * 音声要素を削除する
     *
     * @param delete [AudioAssetData]
     */
    fun deleteAudioAssetData(delete: AudioAssetData) {
        viewModelScope.launch {
            videoEditProjectManager.deleteFile(delete.audioFilePath)
            _audioAssetList.value = audioAssetList.value.filter { it.id != delete.id }
        }
    }

    /**
     * 動画ファイルをセットする。Uriをプロジェクトフォルダへコピーする。
     *
     * @param uri ファイルピッカーで選んだUri
     */
    fun setVideoFile(uri: Uri?) {
        uri ?: return
        val akariProjectData = akariProjectData ?: return
        viewModelScope.launch {
            videoFileData.value?.videoFilePath?.let {
                videoEditProjectManager.deleteFile(it)
            }
            _videoFileData.value = null
            val fileName = MediaStoreTool.getFileName(context, uri) ?: "videofile"
            val videoFile = videoEditProjectManager.addFileToProject(akariProjectData.projectId, uri, fileName)
            _videoFileData.value = VideoFileData(
                videoFilePath = videoFile.path,
                fileName = videoFile.name
            )
        }
    }

    /**
     * 音声ファイルをセットする。Uriをプロジェクトフォルダへコピーする。
     *
     * @param uri ファイルピッカーで選んだ Uri
     */
    fun addAudioFile(uri: Uri?) {
        uri ?: return
        val akariProjectData = akariProjectData ?: return
        viewModelScope.launch {
            val fileName = MediaStoreTool.getFileName(context, uri) ?: "audiofile_${System.currentTimeMillis()}"
            val audioFile = videoEditProjectManager.addFileToProject(akariProjectData.projectId, uri, fileName)
            val audioAssetData = AudioAssetData(
                audioFilePath = audioFile.path,
                fileName = fileName,
                volume = 0.10f
            )
            _audioAssetList.value = audioAssetList.value + audioAssetData
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
            canvasElementList = canvasElementList.value,
            videoFileData = videoFileData.value,
            videoOutputFormat = videoOutputFormat.value,
            audioAssetList = audioAssetList.value
        )
        videoEditProjectManager.saveProjectData(newProjectData)
        return newProjectData
    }

}