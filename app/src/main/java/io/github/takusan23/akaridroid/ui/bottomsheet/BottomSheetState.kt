package io.github.takusan23.akaridroid.ui.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import io.github.takusan23.akaridroid.ui.bottomsheet.data.BottomSheetInitData
import io.github.takusan23.akaridroid.ui.bottomsheet.data.BottomSheetResultData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun rememberBottomSheetState(onResult: (BottomSheetResultData) -> Unit): BottomSheetState {
    val scope = rememberCoroutineScope()
    return remember {
        BottomSheetState(scope, onResult)
    }
}

/**
 * ボトムシートの状態を管理するクラス。
 * ただ Flow を何個か返しているだけ、、、
 *
 * @param scope コルーチンスコープ
 * @param onResult ボトムシートで作業すると[BottomSheetResultData]が流れてきます
 */
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetState(private val scope: CoroutineScope, private val onResult: (BottomSheetResultData) -> Unit) {
    private val _bottomSheetInitData = MutableStateFlow<BottomSheetInitData?>(null)

    /** Compose ボトムシート の状態管理 */
    val modalBottomSheetState = SheetState(
        initialValue = SheetValue.Hidden,
        confirmValueChange = { true },
        skipHiddenState = false,
        skipPartiallyExpanded = false
    )

    /** ボトムシートを開くための初期データ */
    val bottomSheetInitData = _bottomSheetInitData.asStateFlow()

    init {
        // ボトムシートの状態を監視する
        scope.launch {
            snapshotFlow { modalBottomSheetState.currentValue }
                .collect {
                    if (it == SheetValue.Hidden) {
                        close()
                    }
                }
        }
    }

    /** ボトムシートを開く */
    fun open(initData: BottomSheetInitData) {
        scope.launch {
            _bottomSheetInitData.value = initData
            modalBottomSheetState.show()
        }
    }

    /** 結果を返す */
    fun sendResult(resultData: BottomSheetResultData) {
        onResult(resultData)
    }

    /** 閉じる */
    fun close() {
        scope.launch {
            _bottomSheetInitData.value = null
            modalBottomSheetState.hide()
        }
    }
}