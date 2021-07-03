package me.zakharov

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.scenes.scene2d.Actor
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import java.util.*

//enum PherType { none = 0, trail = 1, food_trail = -1, debug = -2 } ;
enum class PherType(val v: Float) {
    none(0f), trail(1f), food_trail(-1f), debug(-2f);

    companion object {
        fun clCode(): String =
            enumValues<PherType>().joinToString(
                separator = ", ",
                prefix = "//generated by host\nenum PherType { ",
                postfix = " };\n"
            ) {
                "%s = %.0f".format(it.name, it.v)
            }
    }
}
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
    private val prog = ctx.createProgramWithSource( this::class.java.getResource("/kernels/decay.kernel").readText() )
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

    //private val pixmapBuffer = Gdx2DPixmap(m.buff, )

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
        //tex?.dispose()
        //tex = matrixDisplay(m)
        tex = null
        super.act(delta)
    }

    private fun map2color(v: Float): Pair<Int, PherType> = when {
        v < -1 -> Pair(Color.rgba8888(-v - 1, 0f, 0f, 1f), PherType.debug)
        v < 0 -> Pair(Color.rgba8888(0f, -v, 0f, 1f), PherType.food_trail)
        v > 0 -> Pair(Color.rgba8888(0f, 0f, v, 1f), PherType.trail)
        else -> Pair(Color.rgba8888(1f ,1f, 1f, 0.2f), PherType.none)
    }

    val displayShader = ShaderProgram(
        Gdx.files.internal("shaders/pheromones.vert"),
        Gdx.files.internal("shaders/pheromones.frag")
    )

    // TODO shader
    private fun matrixDisplay(m: Matrix2d<Float>): Texture {

        //val stats = HashMap<PherType, Int>()

        pixmap.pixels.apply {
            for (y in 0 until pixmap.height) {
                val off = y * pixmap.width * 4
                for( x in 0 until pixmap.width) {
                    val idx = off + x * 4
                    //val c = (abs(buff[x, y]) * 0xff).toByte() // 0 .. 1?
                    // its just a tranformation .map { colors
                    val (c, ty) = map2color(m[x, y])
                    //stats[ty] = stats.getOrDefault(ty, 0) + 1
                    this.putInt(idx, c)
                }
            }
        }
        //d("Redraw $stats")
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

        m.forEach { x, y, v ->
            if (x==0) println()
            when(v) {
                PherType.food_trail.v -> print("f")
                PherType.trail.v -> print("t")
                PherType.debug.v -> print("D")
                PherType.none.v -> print(" ")
            }
        }
    }

    fun requestRedraw() {
        tex?.dispose()
        tex = null
    }
}
