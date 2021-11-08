package me.zakharov.me.zakharov.replicators.model

import me.apemanzilla.ktcl.cl10.enqueueNDRangeKernel
import me.apemanzilla.ktcl.cl10.setArg
import me.zakharov.utils.*
// PARAMS
data class WorldConf(
    val width: Int = 1000,
    val height: Int = 1000,
)

class WorldSystem(val cf: WorldConf) {

    val globalSize = cf.width * cf.height
    //val field = object: Matrix2d<Cell>(cf.width, cf.height, Cell.bytesize) {
        val light = createFloatMatrix2d(cf.width, cf.height)
        val minerals = createFloatMatrix2d(cf.width, cf.height)
        val moisture = createFloatMatrix2d(cf.width, cf.height)
        val cells = createByteMatrix2d(cf.width, cf.height)
/*        override fun get(x: Int, y: Int): Cell {
            return Cell(light[x, y], minerals[x, y], moisture[x, y])
        }

        override fun set(x: Int, y: Int, v: Cell) {
            light[x, y] = v.light
            minerals[x, y] = v.minerals
            moisture[x, y] = v.moisture
        }
    }*/

    private val enlight by lazy { object: Kernel("enlight",
        "%s\n%s".format(
            clCode<GroundType>(),
            WorldSystem::class.java.getResource("/kernels/genoms/world.c")!!.readText()
        )
    ) {
        var time = 0f
        val cls = arrayOf(light, minerals, moisture, cells).map { it.buff.share(ctx) }
        override fun act(delta: Float) {
            time += delta
            var a = 0

            with(kernel) {
                setArg(a++, time)
                setArg(a++, delta)
                setArg(a++, cf.width)
                setArg(a++, cf.height)
                setArg(a++, cls[0].remoteBuff)
                setArg(a++, cls[1].remoteBuff)
                setArg(a++, cls[2].remoteBuff)
                setArg(a++, cls[3].remoteBuff)
            }
            cls.forEach { cmd.enqueueWrite(it) }
            cmd.enqueueNDRangeKernel(kernel, 1, null,
                longArrayOf(globalSize.toLong()), longArrayOf(cf.height.toLong()))
            cls.forEach { cmd.enqueueRead(it) }
        }
    } }

    fun act(delta: Float) {
        enlight.act(delta)
    }


}