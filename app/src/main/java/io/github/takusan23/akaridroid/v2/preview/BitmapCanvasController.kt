package io.github.takusan23.akaridroid.v2.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Bitmap に書き込む Canvas を作って管理する */
class BitmapCanvasController {
    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    private val _latestBitmap = MutableStateFlow(bitmap)

    /** [update]が呼ばれたときに[Bitmap]を Flow で送る */
    val latestBitmap = _latestBitmap.asStateFlow()

    /** [Canvas]を作る */
    fun createCanvas(width: Int, height: Int) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmapCanvas = Canvas(bitmap!!)
    }

    /** [Canvas]を取得して、[latestBitmap]を更新する */
    suspend fun update(draw: suspend (canvas: Canvas) -> Unit) {
        val bitmapCanvas = bitmapCanvas ?: return
        val bitmap = bitmap ?: return

        draw(bitmapCanvas)
        // 多分コピーしないと差分が通知されない
        _latestBitmap.value = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }
}