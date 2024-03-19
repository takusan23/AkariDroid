package io.github.takusan23.akaridroid

import androidx.core.content.FileProvider

/**
 * あかりんく AkaLink 用の [FileProvider]
 * 詳しくは[io.github.takusan23.akaridroid.tool.AkaLinkTool]参照
 */
class AkaLinkFileProvider : FileProvider(R.xml.file_paths)