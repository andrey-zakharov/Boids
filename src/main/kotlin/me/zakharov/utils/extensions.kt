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