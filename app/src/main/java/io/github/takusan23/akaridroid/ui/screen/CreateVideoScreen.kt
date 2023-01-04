package io.github.takusan23.akaridroid.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.takusan23.akaridroid.R

/** 動画作成画面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVideoScreen() {

    val projectName = remember { mutableStateOf("") }
    val videoWidth = remember { mutableStateOf("1280") }
    val videoHeight = remember { mutableStateOf("720") }
    val bitrate = remember { mutableStateOf("1000000") }
    val framerate = remember { mutableStateOf("30") }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(text = "プロジェクトの新規作成") },
                actions = {
                    TextButton(onClick = { /*TODO*/ }) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_done_24), contentDescription = null)
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text(text = "作成")
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
                OutlinedTextField(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    value = projectName.value,
                    onValueChange = { projectName.value = it },
                    label = { Text(text = "名前") }
                )
            }
            item { Divider(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) }
            // 動画の高さなど
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .padding(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        value = videoWidth.value,
                        onValueChange = { videoWidth.value = it },
                        label = { Text(text = "動画の幅") }
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .padding(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        value = videoHeight.value,
                        onValueChange = { videoHeight.value = it },
                        label = { Text(text = "動画の高さ") }
                    )
                }
            }
            // フレームレート
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .padding(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        value = bitrate.value,
                        onValueChange = { bitrate.value = it },
                        label = { Text(text = "ビットレート") }
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .padding(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        value = framerate.value,
                        onValueChange = { framerate.value = it },
                        label = { Text(text = "フレームレート") }
                    )
                }
            }
        }
    }
}