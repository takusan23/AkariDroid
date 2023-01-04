package io.github.takusan23.akaridroid.ui.screen

import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.akaridroid.data.CanvasElementData
import io.github.takusan23.akaridroid.data.CanvasElementType
import io.github.takusan23.akaridroid.ui.component.Timeline
import io.github.takusan23.akaridroid.viewmodel.VideoEditorViewModel

/** 編集画面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    viewModel: VideoEditorViewModel = viewModel(
        factory = VideoEditorViewModel.Factory,
        extras = MutableCreationExtras((LocalViewModelStoreOwner.current as HasDefaultViewModelProviderFactory).defaultViewModelCreationExtras).apply {
            set(VideoEditorViewModel.PROJECT_ID, "xxxx")
        }
    )
) {
    val context = LocalContext.current
    val canvasElementList = viewModel.canvasElementList.collectAsState()

    Scaffold {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {

            Surface(
                modifier = Modifier
                    .padding(10.dp)
                    .aspectRatio(1.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) { }

            Timeline(
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp)
                    .fillMaxWidth(),
                elementList = listOf("文字を動画の上に書く", "Hello World", "あたまいたい")
                    .mapIndexed { index, text -> CanvasElementData(100, 100 * index, 0, 100, CanvasElementType.TextElement(text, Color.RED, 100f)) },
                onElementClick = { element ->
                    Toast.makeText(context, "${element}", Toast.LENGTH_SHORT).show()
                }
            )

        }
    }
}