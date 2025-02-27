package io.github.takusan23.akaricore.graphics.data

/** [io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer]の HDR / SDR 設定 */
enum class AkariGraphicsProcessorDynamicRangeMode {

    /** SDR で描画する */
    SDR,

    /**
     * 10-bit HDR で描画する。
     * HLG 動画用。
     *
     * 色空間が BT.2020
     * ガンマカーブが HLG
     */
    TEN_BIT_HDR_HLG,

    /**
     * 10-bit HDR で描画する。
     * HDR10、HDR10+ 動画用。
     *
     * 色空間が BT.2020
     * ガンマカーブが PQ
     */
    TEN_BIT_HDR_PQ;

    /** HDR かどうか */
    val isHdr: Boolean
        get() = when (this) {
            SDR -> false
            TEN_BIT_HDR_HLG, TEN_BIT_HDR_PQ -> true
        }
}