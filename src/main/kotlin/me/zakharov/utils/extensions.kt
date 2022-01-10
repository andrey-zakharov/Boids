package me.zakharov.utils

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer

fun Byte.toHexString() = "%h".format(this)
fun ByteBuffer.print() {
    for( i in 0 until capacity() ) {
        print("%02x".format(this[i]))
    }
    println()
}

fun ByteArray.print(): String {
    val r = ByteOutputStream()
    val o = PrintStream(r)
    print(o)
    return r.toString()
}

fun ByteArray.print(out: PrintStream = System.out) {
    for( i in 0 until size ) {
        out.print("%02x".format(this[i]))
    }
}

fun FloatBuffer.print() {
    if ( capacity() <= 0 ) return

    for( i in 0 until capacity() ) {
        print("%f;".format(this[i]))
    }
    println("capacity: ${capacity()}")
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

inline fun<reified E: Enum<E>> clCode(): String =
    enumValues<E>().joinToString(
        separator = ", ",
        prefix = "//generated by host\nenum %s { ".format(E::class.java.simpleName),
        postfix = " };\n"
    ) {
        "%s = %d".format(it.name, it.ordinal)
    }

