package io.github.takusan23.akaridroid.ui.screen.setting

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.akaridroid.R
import io.github.takusan23.akaridroid.tool.FontManager
import kotlinx.coroutines.launch
import java.io.File

/** フォント設定画面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSettingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // フォント管理するクラス
    val fontManager = remember { FontManager(context) }
    val fontList = remember { mutableStateOf<List<File>>(emptyList()) }

    fun getFileList() {
        scope.launch { fontList.value = fontManager.getFontList() }
    }

    LaunchedEffect(key1 = Unit) {
        getFileList()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "フォント設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_arrow_back_24px), contentDescription = null)
                    }
                },
                actions = {
                    AddFontButton(
                        onAddFont = { uri ->
                            scope.launch {
                                fontManager.addFont(uri)
                                getFileList()
                            }
                        }
                    )
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->

        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(fontList.value) { fontFile ->
                FontListItem(
                    modifier = Modifier.padding(10.dp),
                    name = fontFile.name,
                    path = fontFile.path,
                    onDelete = {
                        scope.launch {
                            fontFile.delete()
                            getFileList()
                        }
                    }
                )
                Divider()
            }
        }
    }
}

@Composable
private fun FontListItem(
    modifier: Modifier = Modifier,
    name: String,
    path: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 20.sp
            )
            Text(
                text = path,
                fontSize = 14.sp
            )
        }
        OutlinedButton(onClick = onDelete) {
            Text(text = "削除する")
        }
    }
}

@Composable
private fun AddFontButton(
    modifier: Modifier = Modifier,
    onAddFont: (Uri) -> Unit
) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.also { onAddFont(it) } }
    )

    TextButton(
        modifier = modifier,
        onClick = { filePicker.launch(arrayOf("font/*")) }
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_outline_text_fields_24), contentDescription = null)
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = "追加する")
    }
}