package io.github.takusan23.akaricore.audio

internal fun ByteArray.toShort(): Short {
    var result = 0
    for (i in 0 until count()) {
        result = result or (get(i).toUByte().toInt() shl 8 * i)
    }
    return result.toShort()
}

internal fun Short.toByteArray(): ByteArray {
    var l = this.toInt()
    val result = ByteArray(2)
    for (i in 0..1) {
        result[i] = (l and 0xff).toByte()
        l = l shr 8
    }
    return result
}