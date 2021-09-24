package Ants

import me.apemanzilla.ktcl.cl10.*
import me.zakharov.ants.model.Sum
import me.zakharov.utils.createSharedFloatArray
import me.zakharov.utils.print
import org.junit.Test
import java.nio.FloatBuffer

class SumTest {
    val device = getPlatforms()[0].getDefaultDevice()!!
    val ctx = device.createContext()
    val cmd = device.createCommandQueue(ctx)
    @Test
    fun `sum float array`() {
        val raw = arrayOf( 1f, 2f, 3f)
        val a = ctx.createSharedFloatArray(raw.size, KernelAccess.ReadOnly)

        with(a.buff.asFloatBuffer()) {
            raw.forEach { this.put(it) }
        }
        a.buff.asFloatBuffer().print()
        val sumKernel = Sum()
        sumKernel.sum(a)

    }
}

private fun FloatBuffer.print() {
    for( i in 0 until this.capacity() ) {
        print("${this[i]} ")
    }
    println()
}
