package me.zakharov

import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

object Const {
    const val FLOAT_SIZE = 4
    const val FLOAT2_SIZE = FLOAT_SIZE * 2
    const val FLOAT3_SIZE = FLOAT_SIZE * 3
    const val INT_SIZE = 4
    const val BOOL_SIZE = 1
}

fun<T> getTypeSize(c: Class<T>) = when {
    c == Int::class.java || c == IntBuffer::class.java ||
    c == Float::class.java || c == FloatBuffer::class.java -> 4
    c == Boolean::class.java || c == ByteBuffer::class.java -> 1 // actually it should be 1 bit
    else -> throw NotImplementedError(c.toString())
}

inline fun<reified T> getTypeSize() = getTypeSize(T::class.java)