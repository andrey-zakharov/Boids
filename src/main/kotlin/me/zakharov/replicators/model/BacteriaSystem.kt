package me.zakharov.me.zakharov.replicators.model

import com.badlogic.gdx.math.Vector2
import me.apemanzilla.ktcl.cl10.enqueueNDRangeKernel
import me.apemanzilla.ktcl.cl10.finish
import me.apemanzilla.ktcl.cl10.setArg
import me.zakharov.utils.*
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.ceil
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
    val genLen: Int = 80,
)

class BacteriaSystem(val cf: BacteriaConf, val world: WorldSystem): IHeadlessActor {
    val max = world.cf.width * world.cf.height
    val pos by lazy { createShared<IntBuffer>(max * 2) }
    val gen by lazy { createShared<ByteBuffer>(max * GEN_LENGTH) }
    val current_command by lazy { createShared<ByteBuffer>(max) }
    val age by lazy { createShared<FloatBuffer>(max) }
    val energy by lazy { createShared<FloatBuffer>(max) }
    // back index for looking up around
    // -1 - means empty
    // >= 0 = index
    val field = createIntMatrix2d(world.cf.width, world.cf.height).apply {
        forEach {x, y, _ ->
            this[x, y] = -1
        }
    }

    val current_idx_buff by lazy { createShared<IntBuffer>(3).also {
        with(it.buff<IntBuffer>()) { put(0, 0); put(1, 0); put(2, 0) }
    } }
    var current_idx: Int
        get() = current_idx_buff.buff<IntBuffer>().get(0)
        set(v) { current_idx_buff.buff<IntBuffer>().put(0, v) }

    var free_count: Int
        get() = current_idx_buff.buff<IntBuffer>().get(1)
        set(v) { current_idx_buff.buff<IntBuffer>().put(1, v) }

    var able_count: Int // number of bacterias with energy capable to growth
        get() = current_idx_buff.buff<IntBuffer>().get(2)
        set(v) { current_idx_buff.buff<IntBuffer>().put(2, v) }

    fun setField(x: Int, y: Int, v: GroundType) {
        world.cells[x, y] = v.ordinal.toByte()
    }

    fun add(b: Bacteria) {
        this[current_idx++] = b
    }

    fun isAlive(i: Int): Boolean {
        val x = pos.buff<IntBuffer>().get(2*i)
        val y = pos.buff<IntBuffer>().get(2*i+1)
        return field[x, y] == i
    }

    operator fun set(i: Int, b: Bacteria) {
        assert ( i < max )
        val x = b.pos.x.roundToInt()
        val y = b.pos.y.roundToInt()

        pos.buff<IntBuffer>().put(2*i, x)
        pos.buff<IntBuffer>().put(2*i+1, y)
        gen._buff.position(i * GEN_LENGTH)
        gen._buff.put(b.gen)
        current_command.buff<ByteBuffer>().put(i, b.current_command)
        age.buff<FloatBuffer>().put(i, b.age)
        energy.buff<FloatBuffer>().put(i, b.energy)
        field[x, y] = i
    }

    operator fun get(i: Int): Bacteria {
        assert(i < current_idx ) { "trying to get #$i total: $current_idx" }
        return Bacteria(
            pos = Vector2(
                pos.buff<IntBuffer>().get(2*i).toFloat(),
                pos.buff<IntBuffer>().get(2*i+1).toFloat()
            ),
            age = age.buff<FloatBuffer>().get(i),
            current_command = current_command.buff<ByteBuffer>().get(i),
            energy = energy.buff<FloatBuffer>().get(i)
        ).also {
            //gen._buff.copyInto(it.gen.copyInto()
            for( ix in 0 until GEN_LENGTH) {
                it.gen[ix] = gen._buff[ix + i * GEN_LENGTH]
            }
        }
    }

    fun copy(f: Int, t: Int) {
        this[t] = this[f]
    }

    val kernelSource by lazy {
        arrayOf(
            clCode<GroundType>(),
            WorldSystem::class.java.getResource("/kernels/genoms/bacteria.c")!!.readText()
        ).joinToString("\n")
    }
    val kernelDefines by lazy { arrayOf(
        "max_age=${cf.maxAge}.",
        "gen_len=${GEN_LENGTH}",
        "COMMAND_COUNT=${COMMAND.values().size}",
        "CL_LOG_ERRORS=stdout",
    ) }
    val fieldCl by lazy { Kernel.ctx.share(field.buff) }
    val ground by lazy { arrayOf(world.light, world.minerals, world.moisture, world.cells).map { Kernel.ctx.share(it.buff) } }
    var time = 0f

    init {
        arrayOf( pos, gen, current_idx_buff, current_command, age, energy ).forEach { it.attach(Kernel.ctx) }
    }

    val graver by lazy { object: Kernel(
        "graver", kernelSource, kernelDefines = kernelDefines
    ) {
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
                enqueueWrite(current_idx_buff)
                enqueueWrite(pos)
                enqueueWrite(gen)
                enqueueWrite(current_command)
                enqueueWrite(age)
                enqueueWrite(energy)
                enqueueWrite(fieldCl)
                enqueueWrite(ground[0])
                enqueueWrite(ground[3])
                enqueueNDRangeKernel(kernel, current_idx.toLong(), 0)
                finish()
                enqueueRead(ground[3])
                enqueueRead(ground[0])
                enqueueRead(fieldCl)
                enqueueRead(energy)
                enqueueRead(age)
                enqueueRead(current_command)
                enqueueRead(gen)
                enqueueRead(pos)
                enqueueRead(current_idx_buff)
            }
        }
    } }

    val gen_processor by lazy { object: Kernel(
        "gen_processor", kernelSource, kernelDefines = kernelDefines
    ) {
        val cells = longArrayOf(ceil(world.cf.width / 3f).toLong(), ceil(world.cf.height / 3f).toLong())
        val globalSizes =  longArrayOf(cells[0] * 3, cells[1] * 3)
        val globalSize = longArrayOf(globalSizes[0] * globalSizes[1])
        val workSize = longArrayOf(cells[0] * cells[1])
        init {
            println("groups: ${cells.joinToString(", ")}")
            println("gsizes: ${globalSizes.joinToString(", ")}")
            println("global: ${globalSize.joinToString(", ")}")
            println("worksize: ${workSize.joinToString(", ")}")
        }
        override fun act(delta: Float) {


            super.act(delta)
            if ( current_idx == 0 ) return;
            with(kernel) {
                var a = 0
                setArg(a++, time) // 0
                setArg(a++, delta)
                setArg(a++, world.cf.width)
                setArg(a++, world.cf.height)
                setArg(a++, current_idx_buff.remoteBuff) // 4
                setArg(a++, pos.remoteBuff)
                setArg(a++, gen.remoteBuff)
                setArg(a++, current_command.remoteBuff)
                setArg(a++, age.remoteBuff)
                setArg(a++, energy.remoteBuff) // 9
                setArg(a++, fieldCl.remoteBuff)
                setArg(a++, ground[0].remoteBuff)
                setArg(a++, ground[1].remoteBuff)
                setArg(a++, ground[2].remoteBuff)
                setArg(a++, ground[3].remoteBuff) // 15
            }

            with(cmd) {
                enqueueWrite(current_idx_buff)
                enqueueWrite(pos)
                enqueueWrite(gen)
                enqueueWrite(current_command)
                enqueueWrite(age)
                enqueueWrite(energy)
                enqueueWrite(fieldCl)
                enqueueWrite(ground[0])
                enqueueWrite(ground[3])

                /// workgroups by
                /// +-+-+-+-+-+-+
                /// |1|2|3|1|2|3|
                /// +-+-+-+-+-+-+
                /// |4|5|6|4|5|6|
                /// +-+-+-+-+-+-+
                /// |7|8|9|7|8|9|
                /// +-+-+-+-+-+-+
                /// to remove race for next step cell
                for( dy in 0 until 3) {
                    for (dx in 0 until 3) {
                        kernel.setArg(15, dx)
                        kernel.setArg(16, dy)
                        enqueueNDRangeKernel(kernel, globalSize = globalSize[0])
                        finish()
                    }
                }


                enqueueRead(ground[3])
                enqueueRead(ground[0])
                enqueueRead(fieldCl)
                enqueueRead(energy)
                enqueueRead(age)
                enqueueRead(current_command)
                enqueueRead(gen)
                enqueueRead(pos)
                enqueueRead(current_idx_buff)
            }

        }
    } }

    override fun act(delta: Float) {
        try {
            world.act(delta)
            graver.act(delta)

            reshake()

            gen_processor.act(delta)

            checkIntegrity()
        } catch (e: kotlin.Throwable) {
            println(e)
            printDebug()
            throw e
        }
    }

    private fun checkIntegrity() {
        assert( world.cells[0, 10] == GroundType.obstacle.ordinal.toByte() )
        assert(current_idx >= 0 && current_idx <= world.cf.totalCells) { "expected $current_idx not negative and less ${world.cf.totalCells}"}
        val posarray = pos.buff<IntBuffer>()
        for ( i in 0 until current_idx ) {
            assert(field[posarray[i*2], posarray[i*2+1]] == i) {
                "for item #$i: x=${posarray[i*2]}, y=${posarray[i*2+1]} found in field: #${field[posarray[i*2], posarray[i*2+1]]}"
            }
        }

        for ( y in 0 until world.cf.height ) {
            for ( x in 0 until world.cf.width ) {
                if ( field[x, y] == -1 ) continue
                val backi = field[x, y]

                assert( posarray[2*backi] == x ) { "x=$x back index=#$backi x=${posarray[2*backi]}" }
                assert( posarray[2*backi+1] == y ){ "y=$y back index=#$backi y=${posarray[2*backi+1]}" }
            }
        }
    }

    private fun reshake() {
        //while( free_count > 0 ) {
            var i = 0
            var moved = 0
            while ( i < current_idx) {
                if (!isAlive(i)) {
                    if ( i != current_idx - 1 ) { //special case
                        //println("moving #${current_idx - 1} to #$i")
                        copy(current_idx - 1, i)
                    }
                    //free_count --
                    current_idx --
                    moved ++
                } else {
                    i++
                }
            }
        /*if (moved > 0 ) {
            printDebug("after reshake")
        }*/
        //}
    }

    fun printDebug(label: String = "") {
        println("[$label] total: $current_idx")
        for( i in 0 until current_idx ) {
            val b = this[i]
            println("#$i pos: ${b.pos.x}x${b.pos.y} age: ${b.age*100}%")
        }

        for( y in 0 until world.cf.height ) {
            for( x in 0 until world.cf.width ) {
                print("${field[x, y]}\t")
            }
            println()
        }

    }

}