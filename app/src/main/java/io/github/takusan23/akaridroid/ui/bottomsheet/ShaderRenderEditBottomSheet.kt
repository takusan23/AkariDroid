package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaricore.video.GpuShaderImageProcessor
import io.github.takusan23.akaricore.graphics.GlslSyntaxErrorException
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.RenderData
import io.github.takusan23.akaridroid.ui.component.BottomSheetHeader
import io.github.takusan23.akaridroid.ui.component.CommonDialog
import io.github.takusan23.akaridroid.ui.component.MessageCard
import io.github.takusan23.akaridroid.ui.component.RenderItemDisplayTimeEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemPositionEditComponent
import io.github.takusan23.akaridroid.ui.component.RenderItemSizeEditComponent
import kotlinx.coroutines.launch

/** コンパイル結果 */
private sealed interface CompileResult {
    /** コンパイル成功 */
    data object Success : CompileResult

    /** 構文エラー */
    data class SyntaxError(val syntaxErrorMessage: String) : CompileResult

    /** それ以外 */
    data object Error : CompileResult
}

/**
 * [RenderData.CanvasItem.Shader]の編集ボトムシート
 *
 * @param renderItem シェーダーを含む情報
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除時に呼ばれる
 */
@Composable
fun ShaderRenderEditBottomSheet(
    renderItem: RenderData.CanvasItem.Shader,
    onUpdate: (RenderData.CanvasItem.Shader) -> Unit,
    onDelete: (RenderData.CanvasItem.Shader) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val shaderItem = remember { mutableStateOf(renderItem) }

    fun update(copy: (RenderData.CanvasItem.Shader) -> RenderData.CanvasItem.Shader) {
        shaderItem.value = copy(shaderItem.value)
    }

    // コンパイルエラーならエラー。エラー無いなら null
    val compileResult = remember { mutableStateOf<CompileResult?>(null) }
    when (val result = compileResult.value) {
        CompileResult.Success -> CommonDialog(
            title = stringResource(id = R.string.video_edit_bottomsheet_shader_compile_check_ok),
            onClose = { compileResult.value = null }
        )

        CompileResult.Error -> CommonDialog(
            title = stringResource(id = R.string.video_edit_bottomsheet_shader_compile_check_fail),
            onClose = { compileResult.value = null }
        )

        is CompileResult.SyntaxError -> CommonDialog(
            title = stringResource(id = R.string.video_edit_bottomsheet_shader_compile_check_fail_syntax_error),
            message = result.syntaxErrorMessage,
            onClose = { compileResult.value = null }
        )

        null -> {
            // do nothing
        }
    }

    Column(
        modifier = Modifier
            .bottomSheetPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        BottomSheetHeader(
            title = stringResource(id = R.string.video_edit_bottomsheet_shader_title),
            onComplete = { onUpdate(shaderItem.value) },
            onDelete = { onDelete(shaderItem.value) }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = shaderItem.value.name,
            onValueChange = { name -> update { it.copy(name = name) } },
            label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_shader_name)) },
            maxLines = 1
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = shaderItem.value.fragmentShader,
            onValueChange = { fragmentShader -> update { it.copy(fragmentShader = fragmentShader) } },
            label = { Text(text = stringResource(id = R.string.video_edit_bottomsheet_shader_fragment_shader)) }
        )

        // 貼り付け・コンパイル確認
        CodeBlockActionButtons(
            modifier = Modifier.align(Alignment.End),
            onPasteClick = {
                val clipboardText = clipboard.getText()
                if (clipboardText != null) {
                    update { it.copy(fragmentShader = clipboardText.text) }
                }
            },
            onCheckPassCompileClick = {
                // GpuShaderImageProcessor を作っているので高コストかもしれない
                // フラグメントシェーダーのコンパイルって GL スレッド必須？
                scope.launch {
                    val processor = GpuShaderImageProcessor()
                    compileResult.value = try {
                        processor.prepare(
                            fragmentShaderCode = shaderItem.value.fragmentShader,
                            width = shaderItem.value.size.width,
                            height = shaderItem.value.size.height
                        )
                        CompileResult.Success
                    } catch (e: GlslSyntaxErrorException) {
                        CompileResult.SyntaxError(e.syntaxErrorMessage)
                    } catch (e: Exception) {
                        CompileResult.Error
                    } finally {
                        processor.destroy()
                    }
                }
            }
        )

        MessageCard(message = stringResource(id = R.string.video_edit_bottomsheet_shader_description))

        MessageCard(message = stringResource(id = R.string.video_edit_bottomsheet_shader_sorry))

        RenderItemPositionEditComponent(
            position = shaderItem.value.position,
            onUpdate = { position -> update { it.copy(position = position) } }
        )

        RenderItemDisplayTimeEditComponent(
            displayTime = shaderItem.value.displayTime,
            onUpdate = { displayTime -> update { it.copy(displayTime = displayTime) } }
        )

        RenderItemSizeEditComponent(
            size = shaderItem.value.size,
            onUpdate = { size -> update { it.copy(size = size) } }
        )
    }
}

/**
 * 貼り付けボタンと、コンパイル確認ボタン
 *
 * @param modifier [Modifier]
 * @param onPasteClick 貼り付け押した時
 * @param onCheckPassCompileClick コンパイル確認押した時
 */
@Composable
private fun CodeBlockActionButtons(
    modifier: Modifier = Modifier,
    onPasteClick: () -> Unit,
    onCheckPassCompileClick: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPasteClick) {
            Icon(painter = painterResource(id = R.drawable.content_paste_24px), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "貼り付け")
        }

        OutlinedButton(onClick = onCheckPassCompileClick) {
            Icon(painter = painterResource(id = R.drawable.android_akari_droid_shader_icon), contentDescription = null)
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "コンパイル確認")
        }
    }
}