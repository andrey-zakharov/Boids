package me.zakharov

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils.lerp
import com.badlogic.gdx.scenes.scene2d.Actor
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class Pheromones(
        private val ctx: CLContext,
        private val cmd: CLCommandQueue,
        private val width: Int,
        private val height: Int
) : Actor() {
    private val alpha: Float = 0.75f // in 1 sec
    private val thres: Float = 0.9f


    /// opencl stuff
    private val prog = ctx.createProgramWithSource( this::class.java.getResource("/decay.kernel").readText())
            .also { it.build() }

    internal val buff = FloatMatrix2d(width, height).apply {
        /*
        val random = Random(Calendar.getInstance().timeInMillis)
        for (x in 0 until this.width) {
            for (y in 0 until this.height) {
                this[x, y] = when {
                    random.nextFloat() >= 0.9f -> -1.0f
                    random.nextFloat() >= 0.95f -> 1.0f
                    else -> 0f
                }
            }
        }*/
    }

    internal val size = buff.size.toLong()
    internal fun updateFromCl() {
        buff.read(cmd, clOutBuff)
    }

    private val clInBuff = ctx.createBuffer(buff.size.toLong())
    private val clOutBuff = ctx.createBuffer(buff.size.toLong())
    internal val clBuff = clOutBuff

    private val kernel = prog.createKernel("decay_kernel").apply {
        setArg(1, thres)
        setArg(2, clInBuff)
        setArg(3, clOutBuff)
    }
    /// end opencl stuff

    private val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888).apply {
        filter = Pixmap.Filter.NearestNeighbour
    }

    private var tex = Texture(pixmap).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }

    override fun act(delta: Float) {
        with(kernel) {
            setArg(0, lerp(1f, alpha, delta))

        }

        buff.write(cmd, clInBuff)

        cmd.enqueueNDRangeKernel(kernel, buff.width*buff.height.toLong())
        cmd.finish()

        buff.read(cmd, clOutBuff)

        pixmap.pixels.apply {
            for (y in 0 until pixmap.height) {
                val off = y * pixmap.width * 4
                for( x in 0 until pixmap.width) {
                    val idx = off + x * 4
                    //val c = (abs(buff[x, y]) * 0xff).toByte() // 0 .. 1?
                    when {
                        buff[x, y] < 0 -> this.putInt(idx, Color.rgba8888(0f, -buff[x, y], 0f, 1f))
                        buff[x, y] > 0 -> this.putInt(idx, Color.rgba8888(0f, 0f, buff[x, y], 1f))
                        else -> this.putInt(idx, Color.rgba8888(0f ,0f, 0f, 0f))
                    }
                }
            }
        }
        tex.dispose()
        tex = Texture(pixmap)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        //batch?.draw(img, 0f, 0f)
        super.draw(batch, parentAlpha)
        batch?.draw(tex, 0f, 0f, stage.width, stage.height)
    }
}
