package io.github.takusan23.akaridroid

import io.github.takusan23.akaridroid.tool.AvAnalyze
import io.github.takusan23.akaridroid.tool.ClipboardManagerTool
import io.github.takusan23.akaridroid.tool.FontManager
import io.github.takusan23.akaridroid.tool.MediaStoreTool
import io.github.takusan23.akaridroid.tool.ProjectFolderManager
import io.github.takusan23.akaridroid.tool.UriTool
import io.github.takusan23.akaridroid.tool.ViewModelResourceTool
import io.github.takusan23.akaridroid.viewmodel.ProjectListViewModel
import io.github.takusan23.akaridroid.viewmodel.VideoEditorViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * koin を使って DI を行う
 * DI したいクラスをここで定義し、ViewModel 作成時にこれらのインスタンスが DI されるようにする
 */
val akariDroidAppModule = module {
    singleOf(::MediaStoreTool)
    singleOf(::UriTool)
    singleOf(::AvAnalyze)
    singleOf(::ViewModelResourceTool)
    singleOf(::ClipboardManagerTool)

    singleOf(::ProjectFolderManager)
    singleOf(::FontManager)

    viewModelOf(::ProjectListViewModel)
    viewModelOf(::VideoEditorViewModel)
}