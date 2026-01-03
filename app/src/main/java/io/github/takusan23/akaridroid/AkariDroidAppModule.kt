package io.github.takusan23.akaridroid

import io.github.takusan23.akaridroid.tool.ProjectFolderManager
import io.github.takusan23.akaridroid.viewmodel.ProjectListViewModel
import io.github.takusan23.akaridroid.viewmodel.VideoEditorViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * koin を使って DI を行う
 * DI したいクラスをここで定義し、ViewModel 作成時にこれらのインスタンスが DI されるようにする
 */
val akariDroidAppModule = module {
    single { ProjectFolderManager(get()) }

    viewModelOf(::ProjectListViewModel)
    viewModelOf(::VideoEditorViewModel)
}