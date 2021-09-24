package me.zakharov.utils

import me.zakharov.Const
import org.lwjgl.BufferUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class Matrix2d<T>(val width: Int, val height: Int, val elementSize: Int) {
    val buff: ByteBuffer = BufferUtils.createByteBuffer(width * height * elementSize).order(ByteOrder.nativeOrder())
    abstract operator fun get(x: Int, y: Int): T
    abstract operator fun set(x: Int, y: Int, v: T)
    fun clear() {
        buff.rewind()
        for(i in 0 until buff.capacity()) buff.put(i, 0)
    }
    inline fun forEach(block: (x: Int, y: Int, v: T) -> Unit) {
        for ( y in 0 until height ) {
            for (x in 0 until width) {
                block(x, y, this[x, y])
            }
        }
    }
}

fun createFloatMatrix2d(width: Int, height: Int) = object : Matrix2d<Float>(width, height, Const.FLOAT_SIZE) {
    override operator fun get(x: Int, y: Int) = buff.asFloatBuffer().get(y * width + x)
    override operator fun set(x: Int, y: Int, v: Float) { buff.asFloatBuffer().put(y * width + x, v) }
}

fun createByteMatrix2d(width: Int, height: Int) = object : Matrix2d<Byte>(width, height, 1) {
    override operator fun get(x: Int, y: Int) = buff.get(y * width + x)
    override operator fun set(x: Int, y: Int, v: Byte) { buff.put(y * width + x, v) }
}
