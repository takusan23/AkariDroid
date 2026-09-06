package io.github.takusan23.akaridroid.ui.sheet

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** ボトムシートに共通して適用する[Modifier] */
@Composable
fun Modifier.bottomSheetPadding() = this
    .padding(10.dp)
   // .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) // todo これなくてもいい？
