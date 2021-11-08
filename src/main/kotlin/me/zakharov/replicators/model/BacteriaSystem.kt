package me.zakharov.me.zakharov.replicators.model

import com.badlogic.gdx.math.Vector2
import me.apemanzilla.ktcl.cl10.createBuffer
import me.apemanzilla.ktcl.cl10.enqueueNDRangeKernel
import me.apemanzilla.ktcl.cl10.setArg
import me.zakharov.Const.INT_SIZE
import me.zakharov.utils.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.roundToInt

interface IWorld {
    fun forArounds(block: () -> Boolean)
}

data class Bacteria(
    val pos: Vector2,
    val gen: ByteArray = ByteArray(GEN_LENGTH) { 0 },
    var current_command: Byte = 0, // for 80 GEN_LENGTH
    var age: Float = 0f,
    var energy: Float = 1f,
    var cell: Cell = Cell(),
) {
    fun memSave(i: Byte, v: Byte) {

    }

    fun memLoad(i: Byte): Byte {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bacteria

        if (pos != other.pos) return false
        if (!gen.contentEquals(other.gen)) return false
        if (current_command != other.current_command) return false
        if (age != other.age) return false
        if (energy != other.energy) return false
        if (cell != other.cell) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + gen.contentHashCode()
        result = 31 * result + current_command
        result = 31 * result + age.hashCode()
        result = 31 * result + energy.hashCode()
        result = 31 * result + cell.hashCode()
        return result
    }
}

data class BacteriaConf(
    val maxAge: Int = 1000,
)

class BacteriaSystem(val cf: BacteriaConf, val world: WorldSystem): IHeadlessActor {
    val max = world.cf.width * world.cf.height
    val pos by lazy { createShared<IntBuffer>(max * 2) }
    val gen by lazy { createShared<ByteBuffer>(max * GEN_LENGTH) }
    val current_command by lazy { createShared<ByteBuffer>(max) }
    val age by lazy { createShared<FloatBuffer>(max) }
    val energy by lazy { createShared<FloatBuffer>(max) }
    // back index for looking up around
    // 0 - zero means empty
    // > 0 = index + 1
    val field = createIntMatrix2d(world.cf.width, world.cf.height)

    val current_idx_buff by lazy { createShared<IntBuffer>(2).also {
        with(it.buff<IntBuffer>()) { put(0, 0); put(1, 0) }
    } }
    var current_idx: Int
        get() = current_idx_buff.buff<IntBuffer>().get(0)
        set(v) { current_idx_buff.buff<IntBuffer>().put(0, v) }

    var free_count: Int
        get() = current_idx_buff.buff<IntBuffer>().get(1)
        set(v) { current_idx_buff.buff<IntBuffer>().put(0, v) }


    fun add(b: Bacteria) {
        if ( current_idx >= max ) throw Error()
        val x = b.pos.x.roundToInt()
        val y = b.pos.y.roundToInt()

        pos.buff<IntBuffer>().put(2*current_idx, x)
        pos.buff<IntBuffer>().put(2*current_idx+1, y)
        gen._buff.position(current_idx * GEN_LENGTH)
        gen._buff.put(b.gen)
        current_command.buff<ByteBuffer>().put(current_idx, b.current_command)
        age.buff<FloatBuffer>().put(current_idx, b.age)
        energy.buff<FloatBuffer>().put(current_idx, b.energy)
        field[x, y] = current_idx + 1
        current_idx ++
    }

    operator fun get(i: Int): Bacteria {
        assert(i < current_idx )
        return Bacteria(
            pos = Vector2(
                pos.buff<IntBuffer>().get(2*i).toFloat(),
                pos.buff<IntBuffer>().get(2*i+1).toFloat()
            ),
            age = age.buff<FloatBuffer>().get(i)
        )
    }

    val kernelSource by lazy {
        arrayOf(
            clCode<GroundType>(),
            WorldSystem::class.java.getResource("/kernels/genoms/bacteria.c")!!.readText()
        ).joinToString("\n")
    }
    val kernelDefines by lazy { arrayOf("max_age=${cf.maxAge}.", "gen_len=${GEN_LENGTH}", "COMMAND_COUNT=${COMMAND.values().size}") }
    val fieldCl by lazy { Kernel.ctx.share(field.buff) }
    val ground by lazy { arrayOf(world.light, world.minerals, world.moisture, world.cells).map { Kernel.ctx.share(it.buff) } }
    var time = 0f
    val graver by lazy { object: Kernel(
        "graver", kernelSource, kernelDefines = kernelDefines
    ) {
        init {
            arrayOf( pos, gen, current_idx_buff, current_command, age, energy ).map { it.attach(ctx) }
        }

        override fun act(delta: Float) {
            time += delta
            super.act(delta)
            if ( current_idx == 0 ) return;
            with(kernel) {
                var a = 0
                setArg(a++, time)
                setArg(a++, delta)
                setArg(a++, world.cf.width)
                setArg(a++, world.cf.height)
                setArg(a++, current_idx_buff.remoteBuff)
                setArg(a++, pos.remoteBuff)
                setArg(a++, gen.remoteBuff)
                setArg(a++, current_command.remoteBuff)
                setArg(a++, age.remoteBuff)
                setArg(a++, energy.remoteBuff)
                setArg(a++, fieldCl.remoteBuff)
                setArg(a++, ground[0].remoteBuff)
                setArg(a++, ground[1].remoteBuff)
                setArg(a++, ground[2].remoteBuff)
                setArg(a++, ground[3].remoteBuff)
            }

            with(cmd) {
                enqueueWrite(pos)
                //enqueueWrite(gen)
                enqueueWrite(age)
                enqueueWrite(energy)
                enqueueWrite(fieldCl)
                enqueueWrite(ground[0])
                enqueueWrite(ground[3])
                enqueueNDRangeKernel(kernel, current_idx.toLong(), 0)
                enqueueRead(ground[3])
                enqueueRead(ground[0])
                enqueueRead(fieldCl)
                enqueueRead(pos)
                enqueueRead(age)
                enqueueRead(gen)
                enqueueRead(energy)
                enqueueRead(current_command)
            }

            println(free_count);
            //age._buff.slice().also { it.limit(current_idx * INT_SIZE) }.slice().print()
                //println(age.buff<FloatBuffer>()[0])
        }
    } }

    val gen_processor by lazy { object: Kernel(
        "gen_processor", kernelSource, kernelDefines = kernelDefines
    ) {
        override fun act(delta: Float) {
            super.act(delta)
            with(kernel) {

                if ( current_idx == 0 ) return;
                var a = 0
                with(kernel) {
                    var a = 0
                    setArg(a++, time)
                    setArg(a++, delta)
                    setArg(a++, world.cf.width)
                    setArg(a++, world.cf.height)
                    setArg(a++, current_idx_buff.remoteBuff)
                    setArg(a++, pos.remoteBuff)
                    setArg(a++, gen.remoteBuff)
                    setArg(a++, current_command.remoteBuff)
                    setArg(a++, age.remoteBuff)
                    setArg(a++, energy.remoteBuff)
                    setArg(a++, fieldCl.remoteBuff)
                    setArg(a++, ground[0].remoteBuff)
                    setArg(a++, ground[1].remoteBuff)
                    setArg(a++, ground[2].remoteBuff)
                    setArg(a++, ground[3].remoteBuff)
                }

                with(cmd) {
                    //enqueueWrite(pos)
                    //enqueueWrite(gen)
                    enqueueWrite(age)
                    enqueueWrite(fieldCl)
                    enqueueWrite(ground[3])
                    enqueueWrite(current_idx_buff)
                    enqueueNDRangeKernel(kernel, current_idx.toLong(), 0)
                    enqueueRead(ground[3])
                    enqueueRead(fieldCl)
                    enqueueRead(age)
                    enqueueRead(current_idx_buff)
                }
            }

        }
    } }

    override fun act(delta: Float) {
        world.act(delta)
        graver.act(delta)
        gen_processor.act(delta)
    }
}