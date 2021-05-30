package me.zakharov

import me.apemanzilla.ktcl.CLBuffer
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.createBuffer
import me.apemanzilla.ktcl.cl10.enqueueReadBuffer
import me.apemanzilla.ktcl.cl10.enqueueWriteBuffer
import me.zakharov.Const.FLOAT_SIZE
import org.lwjgl.BufferUtils
import java.nio.ByteBuffer

abstract class Matrix2d() {
    abstract val buff: ByteBuffer
    fun write(cmd: CLCommandQueue, inBuff: CLBuffer) {
        buff.rewind()
        cmd.enqueueWriteBuffer(
                to = inBuff,
                from = buff
        )
    }
    fun read(cmd: CLCommandQueue, from: CLBuffer) {
        cmd.enqueueReadBuffer(from, buff)
        buff.rewind()
    }
}
class FloatMatrix2d(val width: Int, val height: Int): Matrix2d() {
    override val buff = BufferUtils.createByteBuffer(width * height * FLOAT_SIZE)
    val size get() = buff.capacity()
    operator fun get(x: Int, y: Int) = buff.asFloatBuffer().get(y * width + x)
    operator fun set(x: Int, y: Int, v: Float) = buff.asFloatBuffer().put(y * width + x, v)
    fun put(arr: FloatArray) = buff.asFloatBuffer().put(arr)
}

class ByteMatrix2d(val width: Int, val height: Int): Matrix2d() {
    override val buff = BufferUtils.createByteBuffer(width * height)
    val size get() = buff.capacity()
    operator fun get(x: Int, y: Int) = buff.get(y * width + x)
    operator fun set(x: Int, y: Int, v: Byte) = buff.put(y * width + x, v)
    fun put(arr: ByteArray) = buff.put(arr)
}