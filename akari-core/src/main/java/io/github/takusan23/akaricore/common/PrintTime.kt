package io.github.takusan23.akaricore.common

import kotlin.system.measureTimeMillis

/** 時間測って println する */
internal inline fun <T> printTime(tag: String, code: () -> T): T {
    val result: T
    val time = measureTimeMillis {
        result = code()
    }
    println("[$tag] $time ms")
    return result
}