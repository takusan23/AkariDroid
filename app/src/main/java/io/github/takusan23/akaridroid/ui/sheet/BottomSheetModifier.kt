package io.github.takusan23.akaridroid.ui.sheet

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** ボトムシートに共通して適用する[Modifier] */
@Composable
fun Modifier.bottomSheetPadding() = this
    .padding(10.dp)
    .verticalScroll(rememberScrollState())
   // .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) // todo これなくてもいい？
