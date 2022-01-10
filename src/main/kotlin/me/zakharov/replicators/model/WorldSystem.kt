package me.zakharov.replicators.model

import me.apemanzilla.ktcl.cl10.enqueueNDRangeKernel
import me.apemanzilla.ktcl.cl10.finish
import me.apemanzilla.ktcl.cl10.setArg
import me.zakharov.Const
import me.zakharov.utils.*
import java.lang.Integer.min
import kotlin.math.PI
import kotlin.math.sin

// PARAMS
data class WorldConf(
    val width: Int = 1000,
    val height: Int = 1000,
    val medianUltraviolet: Float = 0.85f,
) {
    val totalCells by lazy { width * height }
}

data class Cell(
    var light: Float = 0f,
    var minerals: Float = 0f,
    var moisture: Float = 0f,
) {
    companion object {
        val bytesize = Const.FLOAT_SIZE * 3
    }
}

class WorldSystem(val cf: WorldConf, fieldInitDrawer: ((Matrix2d<Byte>) -> Unit)? = null) : IHeadlessActor {

    var time = 0f
    val yearDur = 5 * 60; // 5 min
    val seasonedUltraviolet: Float
        get() =
            // 2pi - is a full year = 5 min
            cf.medianUltraviolet * ( 1 + sin(2 * PI * time / yearDur).toFloat() * 0.25f)

    val globalSize = cf.width * cf.height
    //val field = object: Matrix2d<Cell>(cf.width, cf.height, Cell.bytesize) {
    val light = createFloatMatrix2d(cf.width, cf.height)
    val minerals = createFloatMatrix2d(cf.width, cf.height)
    val moisture = createFloatMatrix2d(cf.width, cf.height)
    val cells = createByteMatrix2d(cf.width, cf.height).apply {
        fieldInitDrawer?.invoke(this) ?: run {
            for(i in 0 until min(cf.width, cf.height)) {
                this[i, i] = GroundType.obstacle.ordinal.toByte()
            }
            if ( cf.width < 5 ) return@run
            //test case #1
            val r = min(10, cf.height-1)
            for (x in 0 until min(15, cf.width)) {
                this[x, r] = GroundType.obstacle.ordinal.toByte()
            }

        }
    }
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

        val cls = arrayOf(light, minerals, moisture, cells).map { it.buff.share(ctx) }
        override fun act(delta: Float) {
            time += delta
            var a = 0

            with(kernel) {
                setArg(a++, time)
                setArg(a++, delta)
                setArg(a++, cf.width)
                setArg(a++, cf.height)
                setArg(a++, seasonedUltraviolet)
                cls.forEach { setArg(a++, it.remoteBuff) }
            }
            cls.forEach { cmd.enqueueWrite(it) }
            cmd.enqueueNDRangeKernel(kernel, 1, null,
                longArrayOf(globalSize.toLong()), longArrayOf(cf.width.toLong()))
            cmd.finish()
            cls.forEach { cmd.enqueueRead(it) }
        }
    } }

    override fun act(delta: Float) {
        enlight.act(delta)
    }



}