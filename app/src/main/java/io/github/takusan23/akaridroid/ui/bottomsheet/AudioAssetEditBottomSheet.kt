package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.foundation.layout.*
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
import io.github.takusan23.akaridroid.data.AudioAssetData
import io.github.takusan23.akaridroid.ui.component.edit.AudioAssetEditForm
import kotlinx.coroutines.launch

/**
 * 音声素材の編集画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioAssetEditBottomSheet(
    initAudioAssetData: AudioAssetData,
    onUpdate: (AudioAssetData) -> Unit,
    onDelete: (AudioAssetData) -> Unit,
    onClose: () -> Unit,
) {
    val audioAssetData = remember { mutableStateOf(initAudioAssetData) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "音声の編集") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_close_24), contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onUpdate(audioAssetData.value)
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
                AudioAssetEditForm(
                    audioAssetData = audioAssetData.value,
                    onUpdate = { update -> audioAssetData.value = update }
                )
            }
            item { Divider(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) }
            item {
                OutlinedButton(
                    modifier = Modifier
                        .padding(start = 10.dp, end = 10.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("本当に削除しますか？", "削除")
                            if (result == SnackbarResult.ActionPerformed) {
                                onDelete(audioAssetData.value)
                            }
                        }
                    }
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24), contentDescription = null)
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = "削除する")
                }
            }
        }
    }

}