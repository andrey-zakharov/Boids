package me.zakharov.utils

import com.badlogic.gdx.math.Vector2
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