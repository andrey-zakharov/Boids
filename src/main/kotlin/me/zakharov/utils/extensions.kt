package me.zakharov.utils

import java.nio.ByteBuffer

fun Byte.toHexString() = "%h".format(this)
fun ByteBuffer.print() {
    for( i in 0 until capacity() ) {
        print("%02x".format(this[i]))
    }
    println()
}

/* Euclidean division
 check :
 https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/divmodnote-letter.pdf
 https://github.com/typelevel/algebra/pull/172#issuecomment-248813229
 https://youtrack.jetbrains.com/issue/KT-14650
 */
fun Int.modE(d: Int): Int {
    var r = this % d
    if (r < 0) {
        r = if (d > 0) r + d else r - d
    }
    return r
}

fun<T> T.formatBytes(): String where T: Number, T: Comparable<T> {
    var r = this.toLong() // could be overflown if from ULong
    val digits = listOf(10e12 to "Tera", 10e9 to "Giga", 10e6 to "Mega", 10e3 to "kilo")
        .map { it.first.toLong() to it.second.first() }.toMutableList()
    return with(mutableListOf<String>()) {
        while (digits.isNotEmpty()) {
            if (r >= digits.first().first) {
                val digitValue = r / digits.first().first
                add("$digitValue${digits.first().second}")
                r -= digitValue * digits.first().first
            }
            digits.removeFirst()
        }
        if ( r > 0 ) {
            add("$r bytes")
        }
        joinToString(" ")
    }
}
