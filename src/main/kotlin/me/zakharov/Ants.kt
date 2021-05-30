package me.zakharov.me.zakharov

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Const.FLOAT_SIZE
import me.zakharov.Pheromones
import org.lwjgl.BufferUtils
import java.util.*
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.random.Random

class Ants(
        private val ctx: CLContext,
        private val cmd: CLCommandQueue,
        val w: Int, val h: Int,
        private val pheromones: Pheromones
        //private val ground:
) : Actor() {
    private val total_count = 100
    private val max_speed = 200.0f ///< per second
    private val angle_degs = 30 ///< angle of detection for ant in degrees
    private val random = Random(Calendar.getInstance().timeInMillis)
    @ExperimentalUnsignedTypes
    private val maxqs = ceil(PI * max_speed * max_speed * angle_degs / 360).toUInt()

    private val prog = ctx.createProgramWithSource( this::class.java.getResource("/ant.kernel").readText())
            .also { it.build("-DMAX_QUEUE_SIZE=$maxqs") }
    private val kernel = prog.createKernel("ant_kernel").also {
        //it.setArg(1, thres)
    }

    private val posBuff = BufferUtils.createByteBuffer(total_count * FLOAT_SIZE * 2)
    private val velBuff = BufferUtils.createByteBuffer(total_count * FLOAT_SIZE * 2)

    private val posCLBuff = ctx.createBuffer(posBuff)
    private val velCLBuff = ctx.createBuffer(velBuff)
    private val outPosCLBuff = ctx.createBuffer(posBuff.capacity().toLong())
    private val outVelCLBuff = ctx.createBuffer(velBuff.capacity().toLong())
    private val outPheromones = ctx.createBuffer(pheromones.size)

    private val shapeRenderer = ShapeRenderer()
    private val tex = Texture("ant.png")

    init {

        val p = posBuff.asFloatBuffer()
        val v = velBuff.asFloatBuffer()

        for (i in 0 until total_count) {
            p.put(i * 2, random.nextFloat() * w)
            p.put(i * 2 + 1, random.nextFloat() * h)

            val vel = Vector2(max_speed, 0f).rotateRad(2*PI.toFloat()*random.nextFloat())
            
            v.put( i * 2, vel.x)
            v.put( i * 2 + 1, vel.y)
        }
    }


    override fun act(delta: Float) {
        super.act(delta)
        val r = (max_speed * delta)
        val s = ceil(PI * r * r * angle_degs / 360)

        with(kernel) {
            var a = 0
            setArg( a++, delta)
            setArg( a++, w)
            setArg( a++, h)
            setArg( a++, pheromones.clBuff)
            setArg( a++, posCLBuff)
            setArg( a++, velCLBuff)
            setArg( a++, outPosCLBuff)
            setArg( a++, outPheromones)
        }

        cmd.enqueueWriteBuffer(posBuff, posCLBuff)
        cmd.enqueueWriteBuffer(velBuff, velCLBuff)
        cmd.enqueueNDRangeKernel(kernel, total_count.toLong())
        cmd.finish()

        cmd.enqueueReadBuffer(outPosCLBuff, posBuff)
        cmd.enqueueReadBuffer(velCLBuff, velBuff)
        pheromones.updateFromCl()
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        batch?.let {
            val p = posBuff.asFloatBuffer()
            for ( i in 0 until p.capacity() / 2 ) {
                it.draw(tex, p[2*i], h - p[2*i+1])
            }
        }

//            with(shapeRenderer) {
//                projectionMatrix = it.projectionMatrix
//                color = Color.RED
//                begin(ShapeRenderer.ShapeType.Line)
//
//                for ( i in 0 until p.capacity() / 2 ) {
//                    point(p[2*i], p[(2*i) + 1], 0f)
//                }
//                end()
//            }
        //}
    }

}
