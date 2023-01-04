package io.github.takusan23.akaridroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.takusan23.akaridroid.data.CanvasElementData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoEditorViewModel(application: Application, private val projectId: String) : AndroidViewModel(application) {

    private val _canvasElementList = MutableStateFlow<List<CanvasElementData>>(listOf())

    val canvasElementList = _canvasElementList.asStateFlow()

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