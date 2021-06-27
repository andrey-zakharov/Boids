package me.zakharov

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*

class Pheromones(
        private val ctx: CLContext,
        private val cmd: CLCommandQueue,
        internal val w: Int,
        internal val h: Int
) : Actor() {

    //suspend fun mapIndexed(buff: Matrix2d<out T>, (x:Int, y: Int, v:T) -> Any? ) {
//
   // }
    private val alpha: Float = 0.75f // in 1 sec
    private val thres: Float = 0.1f


    fun reset(): Boolean {
        m.clear()
        tex?.dispose()
        tex = null
        return true
    }

    /// opencl stuff
    private val prog = ctx.createProgramWithSource( this::class.java.getResource("/decay.kernel").readText() )
            .also { it.build() }

    internal val m = createFloatMatrix2d(w, h)
    internal val shared = ctx.share(m.buff)

    private val kernel = prog.createKernel("decay_kernel").apply {
        setArg(1, thres)
        setArg(2, shared.remoteBuff)
        setArg(3, shared.remoteBuff)
    }
    /// end opencl stuff

    private val pixmap = Pixmap(w, h, Pixmap.Format.RGBA8888).apply {
        filter = Pixmap.Filter.NearestNeighbour
    }

    private var tex: Texture? = null

    override fun act(delta: Float) {
        with(kernel) {
            setArg(0, lerp(1f, alpha, delta))

        }

        shared.upload(cmd)

        cmd.enqueueNDRangeKernel(kernel, m.width*m.height.toLong())
        cmd.finish()

        shared.download(cmd)

        // could we just pass pixmap.pixels.buff directly to cl and read back?
        // draw Matrix on pixmap
        //pixmap = Pixmap(Gdx2DPixmap.newPixmap())
        tex?.dispose()
        tex = matrixDisplay(m)
        super.act(delta)
    }

    // TODO shader
    private fun matrixDisplay(m: Matrix2d<Float>): Texture {

        val stats = HashMap<Int, Int>()

        pixmap.pixels.apply {
            for (y in 0 until pixmap.height) {
                val off = y * pixmap.width * 4
                for( x in 0 until pixmap.width) {
                    val idx = off + x * 4
                    //val c = (abs(buff[x, y]) * 0xff).toByte() // 0 .. 1?
                    // its just a tranformation .map { colors
                    when {
                        m[x, y] < -1 -> this.putInt(idx, Color.rgba8888(-m[x, y] - 1, 0f, 0f, 1f))
                        m[x, y] < 0 -> {this.putInt(idx, Color.rgba8888(0f, -m[x, y], 0f, 1f));
                            stats[0] = stats.getOrDefault(0, 0) + 1
                        }
                        m[x, y] > 0 -> {this.putInt(idx, Color.rgba8888(0f, 0f, m[x, y], 1f));
                            stats[1] = stats.getOrDefault(1, 0) + 1
                        }
                        else -> this.putInt(idx, Color.rgba8888(1f ,1f, 1f, 0.2f))
                    }
                }
            }
        }
        //d(stats)
        return Texture(pixmap).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
        if ( tex == null ) tex = matrixDisplay(m)

        batch?.draw(tex, 0f, 0f, stage.width, stage.height)
    }

    fun print() {

        for(i in 0 until m.buff.capacity()) {
            print("%02x ".format(m.buff.get(i)))
            if ( i % m.width == 0 && i != 0 ) {
                println()
            }
        }
    }
}
