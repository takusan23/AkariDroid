package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.data.CanvasElementType
import io.github.takusan23.akaridroid.ui.component.edit.CanvasElementEditForm
import io.github.takusan23.akaridroid.ui.component.edit.TextEditForm
import kotlinx.coroutines.launch

/**
 * テキスト編集ボトムシート
 *
 * @param initCanvasElementData [CanvasElementData]
 * @param onUpdate 更新時に呼ばれる
 * @param onDelete 削除押したときに呼ばれる
 * @param onClose 閉じる際に呼ぶ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditBottomSheet(
    initCanvasElementData: CanvasElementData,
    onUpdate: (CanvasElementData) -> Unit,
    onDelete: (CanvasElementData) -> Unit,
    onClose: () -> Unit,
) {
    val canvasElementData = remember { mutableStateOf(initCanvasElementData) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "テキストの編集") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_close_24), contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onUpdate(canvasElementData.value)
                            onClose()
                        }
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_done_24), contentDescription = null)
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(text = "適用")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(it),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                when (val elementType = canvasElementData.value.elementType) {
                    is CanvasElementType.TextElement -> {
                        TextEditForm(
                            textElement = elementType,
                            onUpdate = { textElement -> canvasElementData.value = canvasElementData.value.copy(elementType = textElement) }
                        )
                    }
                }
            }
            item { Divider(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) }
            item {
                CanvasElementEditForm(
                    canvasElementData = canvasElementData.value,
                    onUpdate = { elementData -> canvasElementData.value = elementData },
                    onDelete = { elementData ->
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("本当に削除しますか？", "削除")
                            if (result == SnackbarResult.ActionPerformed) {
                                onDelete(elementData)
                            }
                        }
                    }
                )
            }
        }
    }
}