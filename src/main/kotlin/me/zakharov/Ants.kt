package me.zakharov.me.zakharov

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Const.BOOL_SIZE
import me.zakharov.Const.FLOAT2_SIZE
import me.zakharov.Const.FLOAT_SIZE
import me.zakharov.Pheromones
import me.zakharov.me.zakharov.events.PauseEvent
import org.lwjgl.BufferUtils
import java.util.*
import kotlin.math.PI
import kotlin.math.ceil

class Ants(
        private val ctx: CLContext,
        private val cmd: CLCommandQueue,
        private val ground: Ground,
        private val pheromones: Pheromones
) : Actor() {

    private val w = ground.w
    private val h = ground.h

    private val total_count = 1
    private val max_speed = 10.0f ///< per second
    private val angle_degs = 30 ///< angle of detection for ant in degrees
    private val random = Random(Calendar.getInstance().timeInMillis)
    @ExperimentalUnsignedTypes
    private val maxqs = ceil(PI * max_speed * max_speed * angle_degs / 360).toUInt()

    private val prog = ctx.createProgramWithSource( this::class.java.getResource("/ant.cl").readText())
            .also {
                it.build("-DMAX_QUEUE_SIZE=$maxqs")
                println("build ants with MAX_QUEUE_SIZE=$maxqs")

            }
    private val kernel = prog.createKernel("ant_kernel").also {
        //it.setArg(1, thres)
    }

    /// entities
    private val posBuff = BufferUtils.createByteBuffer(total_count * FLOAT2_SIZE)
    private val velBuff = BufferUtils.createByteBuffer(total_count * FLOAT2_SIZE)
    /** 0 - empty, looking for food, 1 - with food */
    private val stateBuff = BufferUtils.createByteBuffer(total_count * BOOL_SIZE)

    private val posCLBuff = ctx.createBuffer(posBuff)
    private val velCLBuff = ctx.createBuffer(velBuff)
    private val stateCLBuff = ctx.createBuffer(stateBuff)
    //private val outPosCLBuff = ctx.createBuffer(posBuff.capacity().toLong())
    //private val outPheromones = ctx.createBuffer(pheromones.size)

    //private val shapeRenderer = ShapeRenderer()
    private val tex = Texture("ant.png")

    init {

        val p = posBuff.asFloatBuffer()
        val v = velBuff.asFloatBuffer()

        for (i in 0 until total_count) {
            p.put(i * 2, random.nextFloat() * w)
            p.put(i * 2 + 1, random.nextFloat() * h)

            //val vel = Vector2(max_speed, 0f).rotateRad(2*PI.toFloat()*random.nextFloat())
            
            //v.put( i * 2, vel.x)
            //v.put( i * 2 + 1, vel.y)
            v.put( i * 2, 0f)
            v.put( i * 2 + 1, 0f)
            //v.put( i * 2, 0f)
            //v.put( i * 2 + 1, max_speed)
        }
    }


    override fun act(delta: Float) {
        super.act(delta)
        //val r = (max_speed * delta)
        //val s = ceil(PI * r * r * angle_degs / 360)

        with(kernel) {
            var a = 0
            setArg( a++, delta)
            setArg( a++, max_speed)
            setArg( a++, w)
            setArg( a++, h)
            setArg( a++, ground.shared.remoteBuff)
            setArg( a++, pheromones.shared.remoteBuff)
            setArg( a++, posCLBuff)
            setArg( a++, velCLBuff)
            setArg( a++, stateCLBuff)
//            setArg( a++, outPosCLBuff)
        }

        /// beforeNDRun()

        with(cmd) {
            enqueueWriteBuffer(posBuff, posCLBuff)
            enqueueWriteBuffer(velBuff, velCLBuff)
            enqueueWriteBuffer(from = stateBuff, to = stateCLBuff)
        }



        cmd.enqueueNDRangeKernel(kernel, total_count.toLong())
        cmd.finish()

        /// afterNDRun
        with(cmd) {
            enqueueReadBuffer(from = posCLBuff, to = posBuff)
            enqueueReadBuffer(from = velCLBuff, to = velBuff)
            enqueueReadBuffer(from = stateCLBuff, to = stateBuff)
        }
        // kernel's flows
        pheromones.shared.download(cmd)
        debugScanning()
        debugObstacles()

    }

    fun debugObstacles() {
        for ( i in 0 until total_count) {
            if ( stateBuff[i] > 0 ) {
                println("$i has ${stateBuff[i]} obstacles")
            }

        }
    }

    fun debugScanning() {
        var r = 0
        for ( y in 9 until pheromones.h ) {
            for (x in 0 until pheromones.w) {
                if ( pheromones.m[x, y] == -1f) r++
            }
        }
        if ( r == 0 ) {
            //this.fire(PauseEvent(pause = true))
            println("EMPTY SCAN")
            val pos = posBuff.asFloatBuffer()
            val vel = velBuff.asFloatBuffer()
            val c = Vector2(pos[0], pos[1])
            val origin = Vector2(pos[0].toInt() + 0.5f, pos[1].toInt() + 0.5f)
            val v = Vector2(vel[0], vel[1]).setLength(1f)
            val min_dot = kotlin.math.cos(23.0 * PI / 180f)

            println("pos = ${pos[0]}x${pos[1]} ($origin)")
            println("vel = ${vel[0]}x${vel[1]}")
            val d = arrayOf(
                    Vector2(-1f, -1f), Vector2(0f, -1f), Vector2(1f, -1f),
                    Vector2(-1f, 0f)                       , Vector2(1f, 0f),
                    Vector2(-1f, 1f), Vector2(0f, 1f), Vector2(1f, 1f)
            )
            for (dd in d) {
                val np = Vector2(origin).add(dd).sub(origin).setLength(1f)
                val dot = np.dot(v)
                println("$dd dot = $dot < $min_dot = ${dot < min_dot}")
            }
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        batch?.let {
            val p = posBuff.asFloatBuffer()
            for ( i in 0 until p.capacity() / 2 ) {
                it.draw(tex, p[2*i] * stage.width / w, (h - p[2*i+1]) * stage.height / h)
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
