package me.zakharov.ants.model

import com.badlogic.gdx.math.MathUtils.lerp
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Resettable
import me.zakharov.utils.Kernel
import me.zakharov.utils.*
import java.io.OutputStream
import java.io.PrintStream
import kotlin.math.ceil

class Sum : Kernel("sum") {
    fun sum(sharedBuffer: SharedBuffer) {
        val global_size = sharedBuffer.buff.asFloatBuffer().capacity().toLong()
        val group_size = global_size
        val total_groups = ceil(global_size / group_size.toFloat()).toInt()
        println("[sum] global_size=$global_size group_size=$group_size total_groups=$total_groups")
        val res = ctx.createSharedFloatArray(total_groups, KernelAccess.WriteOnly)

        with(kernel) {
            setArg(0, sharedBuffer.remoteBuff)
            setArg(1, res.remoteBuff)
        }

        with(cmd) {
            enqueueWrite(sharedBuffer)
            enqueueNDRangeKernel(kernel, 1, null,
                globalWorkSize = LongArray(1) { global_size },
                localWorkSize = LongArray(1) { group_size }
            )
            finish()
            enqueueRead(res)
        }

        res.buff.print()
    }

}

data class PheromonesConfig(
    val alpha: Float = 0.999f, // in 1 sec
    val thres: Float = 0.0001f
)

//enum PherType { none = 0, trail = 1, food_trail = -1, debug = -2 } ;
// @CLCode
enum class PherType(val v: Float) {
    none(0f), trail(1f), food_trail(-1f), debug(-2f);
    /// TBD kotlinc plugin for @CLCode
}
class Pheromones(
        ctx: CLContext,
        private val cmd: CLCommandQueue,
        val width: Int,
        val height: Int,
        kernel: String = Pheromones::class.java.getResource("/kernels/decay.kernel")!!.readText()
) : IHeadlessActor, Resettable {

    private val alpha: Float = 0.95f // in 1 sec
    private val thres: Float = 0.001f

    override fun reset(): Boolean {
        m.clear()
        return true
    }

    /// opencl stuff
    private val prog = ctx.createProgramWithSource( kernel )
            .also { it.build() }

    internal val m = createFloatMatrix3d(width, height, 2)
    internal val shared = ctx.share(m.buff)

    private val kernel = prog.createKernel("decay_kernel").apply {
        setArg(1, thres)
        setArg(2, shared.remoteBuff)
        setArg(3, shared.remoteBuff)
    }
    /// end opencl stuff


    override fun act(delta: Float) {
        with(kernel) {
            setArg(0, lerp(1f, alpha, delta))
        }

        with(cmd) {
            enqueueWriteBuffer(shared.buff, shared.remoteBuff)
            enqueueNDRangeKernel(kernel, 2*m.width*m.height.toLong())
            finish()
            enqueueReadBuffer(shared.remoteBuff, shared.buff)
        }
    }


    fun print() {
        val buff = OutToStringBuilderStream()
        val b = "+" + "=".repeat(width) + "+"
        with(PrintStream(buff)) {
            print(b)
            m.forEach { x, y, z, v ->
                if ( x == 0 ) {
                    if ( y != 0 ) print("!")
                    println()
                    print("!")
                }
                print(when {
                    v == 0f -> " "
                    v < 0f -> "f" // PherType.food_trail.v
                    v > 0f -> "." //PherType.trail.v
                    v < -1f -> "D" // PherType.debug.v
                    else -> throw RuntimeException(v.toString())
                })
            }
            println("!")
            print(b)
        }
        println(buff.toStr)
    }
}
