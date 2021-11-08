package me.zakharov.utils

import me.zakharov.Const
import me.zakharov.getTypeSize
import org.lwjgl.BufferUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

abstract class Matrix2dProjector {
    abstract val width: Int
    abstract val height: Int
    fun getIndexFromCoords(x: Int, y: Int): Int = x + y * width
    fun getCoordsFromIndex(i: Int): Pair<Int, Int> = Pair( i % width, i / width )

    //fun getTypedBuffer()
}

inline fun<reified T> ByteBuffer.asBuff(): java.nio.Buffer = when {
    T::class == Float::class || T::class == FloatBuffer::class -> asFloatBuffer()
    T::class == Byte::class || T::class == ByteBuffer::class -> this
    T::class == Int::class || T::class == IntBuffer::class -> asIntBuffer()
    else -> throw NotImplementedError(T::class.toString())
}

abstract class Matrix2d<T> constructor(
    val width: Int, val height: Int, val elementSize: Int,
    val buff: ByteBuffer = BufferUtils.createByteBuffer(width * height * elementSize).order(ByteOrder.nativeOrder())
) {
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
    // tbd kernel invoke on some kernel
}

abstract class Matrix3d<T>(val width: Int, val height: Int, val depth: Int, private val elementSize: Int) {
    val s = width * height
    val buff: ByteBuffer = BufferUtils.createByteBuffer(depth * s * elementSize).order(ByteOrder.nativeOrder())
    abstract operator fun get(x: Int, y: Int, z: Int): T
    abstract operator fun set(x: Int, y: Int, z: Int, v: T)
    fun clear() {
        buff.rewind()
        for(i in 0 until buff.capacity()) buff.put(i, 0)
    }
    inline fun forEach(block: (x: Int, y: Int, z: Int, v: T) -> Unit) {
        for ( z in 0 until depth ) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    block(x, y, z, this[x, y, z])
                }
            }
        }
    }
}
fun createIntMatrix2d(width: Int, height: Int) = object : Matrix2d<Int>(width, height, Const.INT_SIZE) {
    val ib = buff.asIntBuffer()
    override operator fun get(x: Int, y: Int) = ib.get(y * width + x)
    override operator fun set(x: Int, y: Int, v: Int) { ib.put(y * width + x, v) }
}

fun createFloatMatrix2d(width: Int, height: Int) = object : Matrix2d<Float>(width, height, Const.FLOAT_SIZE) {
    override operator fun get(x: Int, y: Int) = buff.asFloatBuffer().get(y * width + x)
    override operator fun set(x: Int, y: Int, v: Float) { buff.asFloatBuffer().put(y * width + x, v) }
}

fun createFloatMatrix3d(width: Int, height: Int, depth: Int) = object : Matrix3d<Float>(width, height, depth, Const.FLOAT_SIZE) {
    val sq = width * height
    override operator fun get(x: Int, y: Int, z: Int) = buff.asFloatBuffer().get(sq * z + y * width + x)
    override operator fun set(x: Int, y: Int, z: Int, v: Float) {
        buff.asFloatBuffer().put(sq * z + y * width + x, v)
    }
}
fun createByteMatrix2d(width: Int, height: Int) = object : Matrix2d<Byte>(width, height, 1) {
    override operator fun get(x: Int, y: Int) = buff.get(y * width + x)
    override operator fun set(x: Int, y: Int, v: Byte) { buff.put(y * width + x, v) }
}
