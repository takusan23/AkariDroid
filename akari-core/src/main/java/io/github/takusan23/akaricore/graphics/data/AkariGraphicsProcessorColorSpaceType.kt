package io.github.takusan23.akaricore.graphics.data

/** [io.github.takusan23.akaricore.graphics.AkariGraphicsTextureRenderer]の HDR / SDR 設定 */
enum class AkariGraphicsProcessorColorSpaceType {

    /** SDR で描画する */
    SDR_BT709,

    /**
     * 10-bit HDR で描画する。
     * HLG 動画用。
     *
     * 色空間が BT.2020
     * ガンマカーブが HLG
     */
    TEN_BIT_HDR_BT2020_HLG,

    /**
     * 10-bit HDR で描画する。
     * HDR10、HDR10+ 動画用。
     *
     * 色空間が BT.2020
     * ガンマカーブが PQ (ST2084)
     */
    TEN_BIT_HDR_BT2020_PQ;

    /** HDR かどうか */
    val isHdr: Boolean
        get() = when (this) {
            SDR_BT709 -> false
            TEN_BIT_HDR_BT2020_HLG, TEN_BIT_HDR_BT2020_PQ -> true
        }
}