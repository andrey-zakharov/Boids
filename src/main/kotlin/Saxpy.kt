package me.zakharov

import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Const.FLOAT_SIZE
import org.lwjgl.BufferUtils

class Saxpy(
        private val ctx: CLContext,
        private val cmd: CLCommandQueue,
        private val alpha: Float = 1.0f
) {

    private val prog = ctx.createProgramWithSource( this::class.java.getResource("/saxpy.kernel").readText())
            .also { it.build() }
    private val inA = ctx.createBuffer(5 * FLOAT_SIZE.toLong()).also { println(it) }
    private val inB = ctx.createBuffer(5 * FLOAT_SIZE.toLong()).also { println(it) }
    private val out = ctx.createBuffer(5 * FLOAT_SIZE.toLong()).also { println(it) }

    private val kernel = prog.createKernel("saxpy_kernel").also {
        it.setArg(0, alpha)
        it.setArg(1, inA)
        it.setArg(2, inB)
        it.setArg(3, out)
    }

    init {


    }

    fun act() {
        cmd.enqueueWriteBuffer(
                to = inA,
                from = BufferUtils.createByteBuffer(5 * FLOAT_SIZE).apply {
                    asFloatBuffer().put(floatArrayOf(1f, 2f, 3f, 4f, 5f)).flip()
                }
        )

        cmd.enqueueWriteBuffer(
                to = inB,
                from = BufferUtils.createByteBuffer(5 * FLOAT_SIZE).apply {
                    asFloatBuffer().put(floatArrayOf(2f, 4f, 6f, 8f, 10f)).flip()
                }
        )

        cmd.enqueueNDRangeKernel(kernel, 5)

        val outData = BufferUtils.createByteBuffer(5 * FLOAT_SIZE)
        cmd.enqueueReadBuffer(out, outData)

        val res = outData.asFloatBuffer()

        for (i in 0 until res.capacity()) {
            println("$i = ${res[i]}")
        }
    }
}