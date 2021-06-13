package me.zakharov

import me.apemanzilla.ktcl.CLBuffer
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Const.FLOAT_SIZE
import org.lwjgl.BufferUtils
import java.nio.Buffer
import java.nio.ByteBuffer

interface SharedBuffer {
    val buff: ByteBuffer
    val remoteBuff: CLBuffer
    fun upload(cmd: CLCommandQueue)
    fun download(cmd: CLCommandQueue)
}

fun CLContext.share(buff: ByteBuffer, memFlag: MemFlag = KernelAccess.ReadWrite) = object : SharedBuffer {
    override val buff = buff
    val bytesSize = buff.capacity()
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


abstract class Matrix2d<T>(val width: Int, val height: Int) {
    abstract operator fun get(x: Int, y: Int): T
    abstract operator fun set(x: Int, y: Int, v: T)
}
abstract class MatrixByteBuffer<T>(val wrapped: ByteBuffer, width: Int, height: Int) : Matrix2d<T>(width, height)

fun createFloatMatrix2d(width: Int, height: Int): MatrixByteBuffer<Float> {
    val bytesSize: Int = FLOAT_SIZE * width * height
    return BufferUtils.createByteBuffer(bytesSize).asFloatMatrix2d(width, height)
}
fun createByteMatrix2d(width: Int, height: Int): MatrixByteBuffer<Byte> {
    val bytesSize: Int = width * height
    return BufferUtils.createByteBuffer(bytesSize).asByteMatrix2d(width, height)
}

// TBD sampler
@Suppress("CAST_NEVER_SUCCEEDS")
fun ByteBuffer.asFloatMatrix2d(width: Int, height: Int): MatrixByteBuffer<Float> =
        object : MatrixByteBuffer<Float>(this, width, height) {
    override operator fun get(x: Int, y: Int) = asFloatBuffer().get(y * width + x)
    override operator fun set(x: Int, y: Int, v: Float) { asFloatBuffer().put(y * width + x, v) }
}

@Suppress("CAST_NEVER_SUCCEEDS")
fun ByteBuffer.asByteMatrix2d(width: Int, height: Int) : MatrixByteBuffer<Byte> =
        object : MatrixByteBuffer<Byte>(this, width, height) {
    override operator fun get(x: Int, y: Int) = get(y * width + x)
    override operator fun set(x: Int, y: Int, v: Byte) { put(y * width + x, v) }
}