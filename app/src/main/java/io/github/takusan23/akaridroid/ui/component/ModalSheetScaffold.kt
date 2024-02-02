package io.github.takusan23.akaridroid.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * モーダルシートとかが追加できる Scaffold
 */
@ExperimentalMaterial3Api
@Composable
fun ModalSheetScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    modalBottomSheetState: SheetState = rememberModalBottomSheetState(),
    onDismissBottomSheet: () -> Unit = {},
    contentColor: Color = contentColorFor(containerColor),
    bottomSheetContent: @Composable() (ColumnScope.() -> Unit) = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        Box(modifier = Modifier.padding(it)) {
            content()
            SnackbarHost(
                modifier = Modifier.align(Alignment.BottomCenter),
                hostState = snackbarHostState
            )
        }

        // ModalBottomSheetLayout がないので
        if (modalBottomSheetState.isVisible) {
            ModalBottomSheet(
                sheetState = modalBottomSheetState,
                onDismissRequest = onDismissBottomSheet
            ) {
                bottomSheetContent()
            }
        }
    }
}