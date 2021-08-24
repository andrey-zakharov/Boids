package me.zakharov

import me.apemanzilla.ktcl.CLBuffer
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Const.FLOAT_SIZE
import org.lwjgl.BufferUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface SharedBuffer {
    val buff: ByteBuffer
    val remoteBuff: CLBuffer
    fun upload(cmd: CLCommandQueue)
    fun download(cmd: CLCommandQueue)
}


//require
fun CLContext.share(buff: ByteBuffer, memFlag: MemFlag = KernelAccess.ReadWrite) = object : SharedBuffer {
    override val buff = buff
    val bytesSize = buff.capacity()
    init { require(bytesSize > 0) }
    override val remoteBuff = createBuffer(bytesSize.toLong(), memFlag)
    override fun upload(cmd: CLCommandQueue) {
        buff.enqueueWrite(cmd, remoteBuff)
    }
    override fun download(cmd: CLCommandQueue) {
        buff.enqueueRead(cmd, remoteBuff)
    }
}

fun ByteBuffer.enqueueWrite( cmd: CLCommandQueue, remoteBuff: CLBuffer ) {
    rewind()
    cmd.enqueueWriteBuffer(
            to = remoteBuff,
            from = this
    )
}

fun ByteBuffer.enqueueRead( cmd: CLCommandQueue, remoteBuff: CLBuffer) {
    cmd.enqueueReadBuffer(remoteBuff, this)
    rewind()
}

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

fun createFloatMatrix2d(width: Int, height: Int) = object : Matrix2d<Float>(width, height, FLOAT_SIZE) {
    override operator fun get(x: Int, y: Int) = buff.asFloatBuffer().get(y * width + x)
    override operator fun set(x: Int, y: Int, v: Float) { buff.asFloatBuffer().put(y * width + x, v) }
}

fun createByteMatrix2d(width: Int, height: Int) = object : Matrix2d<Byte>(width, height, 1) {
    override operator fun get(x: Int, y: Int) = buff.get(y * width + x)
    override operator fun set(x: Int, y: Int, v: Byte) { buff.put(y * width + x, v) }
}