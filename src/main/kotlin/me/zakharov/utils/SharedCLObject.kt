package me.zakharov.utils

import me.apemanzilla.ktcl.CLBuffer
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Const
import me.zakharov.getTypeSize
import org.lwjgl.BufferUtils
import java.nio.Buffer
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
inline fun<reified T: Buffer> createShared(s: Int): Shared<T> {
    return Shared(BufferUtils.createByteBuffer(s * getTypeSize<T>()))
}

class Shared<T: Buffer> {
    val _buff: ByteBuffer
    //val buff get(): T = _buff.asBuff<T>() as T
    inline fun<reified R: T> buff(): R = _buff.asBuff<R>() as R
    val mf: MemFlag
    lateinit var remoteBuff: CLBuffer
    fun attach(ctx: CLContext) {
        remoteBuff = ctx.createBuffer(_buff, mf)
    }
    constructor(size: Int, memFlag: MemFlag = KernelAccess.ReadWrite) {
        _buff = BufferUtils.createByteBuffer(size * getTypeSize<Class<T>>())
        this.mf = memFlag
    }

    constructor(buff: ByteBuffer, memFlag: MemFlag = KernelAccess.ReadWrite) {
        _buff = buff
        this.mf = memFlag
    }
}

inline fun<reified T: Buffer> CLCommandQueue.enqueueWrite(b: Shared<T>) {
    b._buff.rewind()
    enqueueWriteBuffer(from = b._buff, to = b.remoteBuff)
}

fun<T: Buffer> CLCommandQueue.enqueueRead(b: Shared<T>) {
    enqueueReadBuffer(b.remoteBuff, b._buff)
    b._buff.rewind()
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

fun ByteBuffer.share(ctx: CLContext, memFlag: MemFlag = KernelAccess.ReadWrite) = ctx.share(this, memFlag)
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
