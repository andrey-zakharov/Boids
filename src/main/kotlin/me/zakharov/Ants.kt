package me.zakharov.me.zakharov

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Const.BOOL_SIZE
import me.zakharov.Const.FLOAT2_SIZE
import me.zakharov.PherType
import me.zakharov.Pheromones
import me.zakharov.d
import me.zakharov.events.PauseEvent
import me.zakharov.warn
import org.lwjgl.BufferUtils
import java.util.*
import kotlin.math.PI
import kotlin.math.ceil

data class AntConfig(
    val angleDegs: Float = 30f
)

data class Ant(val conf: AntConfig, val pos: Vector2, val vel: Vector2) {

    // static config
    // max_angle_lookup

    operator fun invoke(shapes: ShapeRenderer?) {
        shapes?.apply {
            // save state
            val old = color
            color = Color.RED

            val viewVel = vel
            rectLine(pos, pos + viewVel, 10f)

            color = Color.GOLD
            val lBound = pos + viewVel.rotatedDeg(-conf.angleDegs)
            val rBound = pos + viewVel.rotatedDeg(conf.angleDegs)
            triangle(pos.x, pos.y, lBound.x, lBound.y, rBound.x, rBound.y)

            // load state
            color = old
        }
    }
    operator fun invoke(draw: Batch?) {

    }
}

internal fun Vector2.flipY() = Vector2(this.x, -this.y)
internal operator fun Vector2.times(times: Float) = Vector2(this.x * times, this.y * times)
internal operator fun Vector2.plus(pos: Vector2) = Vector2(this.x + pos.x, this.y + pos.y)
internal fun Vector2.rotatedDeg(deg: Float): Vector2 = Vector2(this).rotateDeg(deg)

class Ants(
    private val ctx: CLContext,
    private val cmd: CLCommandQueue,
    private val ground: Ground,
    private val pheromones: Pheromones,
    private val font: BitmapFont,
    // config
    private val angleDegs: Float = 30f ///< angle of detection for ant in degrees
) : Actor() {


    private val w = ground.w
    private val h = ground.h

    private val totalCount = 1
    private val maxSpeed = 10.0f ///< per second

    // hack for cl compile
    //override fun getDebug() = true

    private val random = Random(Calendar.getInstance().timeInMillis)
    @ExperimentalUnsignedTypes
    private val maxQueueSize = ceil(PI * maxSpeed * maxSpeed * angleDegs / 360).toUInt()

    private val prog = ctx.createProgramWithSource(
        this::class.java.getResource("/queue.c").readText(),
        PherType.clCode(),
        this::class.java.getResource("/ant.c").readText()
    )
            .also {
                // WITH_FALLBACK_PATHFINDING=true
                val opts = mutableListOf("-DMAX_QUEUE_SIZE=$maxQueueSize")
                if ( debug ) opts.add("-DDEBUG")

                it.build(opts.joinToString(" "))
                d("build ants with $opts")
            }
    private val kernel = prog.createKernel("ant_kernel").also {
        //it.setArg(1, thres)
    }

    /// entities
    private val posBuff = BufferUtils.createByteBuffer(totalCount * FLOAT2_SIZE)
    private val velBuff = BufferUtils.createByteBuffer(totalCount * FLOAT2_SIZE)
    /** 0 - empty, looking for food, 1 - with food */
    /// TODO bitmask
    private val stateBuff = BufferUtils.createByteBuffer(totalCount * BOOL_SIZE)

    private val posCLBuff = ctx.createBuffer(posBuff)
    private val velCLBuff = ctx.createBuffer(velBuff)
    private val stateCLBuff = ctx.createBuffer(stateBuff)
    //private val outPosCLBuff = ctx.createBuffer(posBuff.capacity().toLong())
    //private val outPheromones = ctx.createBuffer(pheromones.size)

    //private val shapeRenderer = ShapeRenderer()
    private val tex = Texture("carpenter-ant-small.png")


    init {

        val p = posBuff.asFloatBuffer()
        val v = velBuff.asFloatBuffer()

        for (i in 0 until totalCount) {
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
/*
    Texture(White, empty, 100, 100) {

        drawLine(CellType.Obstacle.Color, 0, 0, 50, 50)
    }.toGround()
 */

    override fun act(delta: Float) {
        super.act(delta)
        //val r = (max_speed * delta)
        //val s = ceil(PI * r * r * angle_degs / 360)

        with(kernel) {
            var a = 0
            setArg( a++, delta)
            setArg( a++, maxSpeed)
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



        cmd.enqueueNDRangeKernel(kernel, totalCount.toLong())
        cmd.finish()

        /// afterNDRun
        with(cmd) {
            enqueueReadBuffer(from = posCLBuff, to = posBuff)
            enqueueReadBuffer(from = velCLBuff, to = velBuff)
            enqueueReadBuffer(from = stateCLBuff, to = stateBuff)
        }
        // kernel's flows
        pheromones.shared.download(cmd)
        pheromones.requestRedraw()
        //debugScanning()
        //debugObstacles()

    }

    fun debugObstacles() {
        val p = posBuff.asFloatBuffer()
        for ( i in 0 until totalCount) {

            val pos = Vector2(p[i*2], p[i*2+1])
            if ( stateBuff[i] > 0 ) {
                d("$i $pos has ${stateBuff[i]} obstacles")
            }

        }
    }

    fun debugScanning() {
        var r = 0
        pheromones.m.forEach { x, y, v ->
            if ( v == PherType.food_trail.v) r++
        }

        if ( r == 0 ) {
            this.fire(PauseEvent(pause = true))
            warn("EMPTY SCAN")
            val pos = posBuff.asFloatBuffer()
            val vel = velBuff.asFloatBuffer()
            val c = Vector2(pos[0], pos[1])
            val origin = Vector2(pos[0].toInt() + 0.5f, pos[1].toInt() + 0.5f)
            val v = Vector2(vel[0], vel[1]).setLength(1f)
            val minDot = kotlin.math.cos(23.0 * PI / 180f)

            warn("pos = ${pos[0]}x${pos[1]} ($origin)")
            warn("vel = ${vel[0]}x${vel[1]}")
            val d = arrayOf(
                    Vector2(-1f, -1f), Vector2(0f, -1f), Vector2(1f, -1f),
                    Vector2(-1f, 0f)                       , Vector2(1f, 0f),
                    Vector2(-1f, 1f), Vector2(0f, 1f), Vector2(1f, 1f)
            )
            for (dd in d) {
                val np = Vector2(origin).add(dd).sub(origin).setLength(1f)
                val dot = np.dot(v)
                warn("$dd dot = $dot >= $minDot = ${dot >= minDot}")
            }

            pheromones.print()
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
        val scaleStageX = stage.width / w.toFloat()
        val scaleStageY = stage.height / h.toFloat()

        batch?.let {
            val p = posBuff.asFloatBuffer()
            val v = velBuff.asFloatBuffer()
            for ( i in 0 until p.capacity() / 2 ) {
                val pos = Vector2(p[2*i] * scaleStageX, (h - p[2*i+1]) * scaleStageY )
                val vel = Vector2(v[2*i], v[2*i+1])

                // pos2world
                it.draw(
                    tex,
                    pos.x - tex.width / 2f,
                    pos.y - tex.height / 2f,
                    tex.width / 2f, tex.height / 2f,
                    tex.width.toFloat(), tex.height.toFloat(),
                    1f, 1f, 90 - vel.angleDeg(),
                    0, 0,
                    tex.width, tex.height,
                    false, true
                )

                //font.draw(batch, "%.2fx%.2f".format(p[2*i], p[2*i+1]), pos.x, pos.y + 20f )
                font.draw(batch, "%.2fx%.2f".format(vel.x, vel.y), pos.x, pos.y + 20f )


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

    fun forEach(block: (pos: Vector2, vel: Vector2) -> Unit) {
        val scaleStageX = stage.width / w.toFloat()
        val scaleStageY = stage.height / h.toFloat()
        val p = posBuff.asFloatBuffer()
        val v = velBuff.asFloatBuffer()
        for ( i in 0 until p.capacity() / 2 ) {
            val pos = Vector2(p[2 * i] * scaleStageX, (h - p[2 * i + 1]) * scaleStageY)
            val vel = Vector2(v[2 * i] * scaleStageX, -v[2 * i + 1] * scaleStageY)
            block(pos, vel)
        }
    }

    override fun drawDebug(shapes: ShapeRenderer?) {
        super.drawDebug(shapes)
        forEach { pos, vel ->
            Ant(AntConfig(angleDegs), pos, vel).invoke(shapes)
        }
    }

}
