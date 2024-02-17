package io.github.takusan23.akaridroid.v2.tool

import kotlin.system.measureTimeMillis

/** 時間測って println する */
inline fun <T> String.measureTime(code: () -> T): T {
    val result: T
    val time = measureTimeMillis {
        result = code()
    }
    println("[$this] $time ms")
    return result
}