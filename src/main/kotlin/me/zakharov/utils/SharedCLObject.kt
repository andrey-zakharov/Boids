package me.zakharov.utils

import me.apemanzilla.ktcl.CLBuffer
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Const
import org.lwjgl.BufferUtils
import java.nio.ByteBuffer

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

interface SharedBuffer {
    val buff: ByteBuffer
    val remoteBuff: CLBuffer
    fun upload(cmd: CLCommandQueue)
    fun download(cmd: CLCommandQueue)
}

fun CLCommandQueue.enqueueWrite(b: SharedBuffer) {
    b.buff.rewind()
    enqueueWriteBuffer(from = b.buff, to = b.remoteBuff)
}

fun CLCommandQueue.enqueueRead(b: SharedBuffer) {
    enqueueReadBuffer(b.remoteBuff, b.buff)
    b.buff.rewind()
}


fun CLContext.createSharedFloatArray(size: Int, memFlag: MemFlag = KernelAccess.ReadWrite) =
    this.share(BufferUtils.createByteBuffer(size * Const.FLOAT_SIZE), memFlag)

fun CLContext.createSharedFloat3Array(size: Int) =
    this.share(BufferUtils.createByteBuffer(size * Const.FLOAT3_SIZE))

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
