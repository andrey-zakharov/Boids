package me.zakharov.ants.model

import com.badlogic.gdx.math.Vector2
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Const.BOOL_SIZE
import me.zakharov.Const.FLOAT2_SIZE
import me.zakharov.d
import me.zakharov.utils.IHeadlessActor
import me.zakharov.warn
import org.lwjgl.BufferUtils
import java.util.*
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.system.measureTimeMillis

enum class AntState(val value: Byte) {
    empty(0), full(1);
    companion object {
        private val map = values().associateBy(AntState::value)
        fun fromValue(type: Byte) = map[type] ?: throw IllegalArgumentException()
    }
}

internal operator fun Vector2.times(times: Float) = Vector2(this.x * times, this.y * times)
internal operator fun Vector2.plus(pos: Vector2) = Vector2(this.x + pos.x, this.y + pos.y)
internal fun Vector2.rotatedDeg(deg: Float): Vector2 = Vector2(this).rotateDeg(deg)
internal fun Vector2.normalized(): Vector2 = Vector2(this).nor()

data class AntSnapshot(val pos: Vector2, val vel: Vector2, val state: AntState) {
    override fun toString(): String = "ant [$pos $vel $state]"
}

data class AntsConfig(
    val width: Int,
    val height: Int,
    val totalCount: Int = 200,
    val maxSpeed: Float = 20.0f, ///< per second
    val angleDegs: Float = 40f, ///< angle of detection for ant in degrees
    val debug: Boolean = false
)

class Ants(
    val conf: AntsConfig,
    private val ctx: CLContext,
    private val cmd: CLCommandQueue,
    private val ground: Ground,
    private val pheromones: Pheromones = Pheromones(ctx, cmd, conf.width, conf.height),
) : IHeadlessActor {
    var debug = conf.debug

    // internal conf
    val width = ground.width
    val height = ground.height
    private var time = 0f

    // hack for cl compile
    //override fun getDebug() = true

    private val random = Random(Calendar.getInstance().timeInMillis)
    @ExperimentalUnsignedTypes
    private val maxQueueSize = ceil(PI * conf.maxSpeed * conf.maxSpeed * conf.angleDegs / 360f).toUInt().coerceAtLeast(8u)

    @ExperimentalUnsignedTypes
    private val prog = ctx.createProgramWithSource(
        this::class.java.getResource("/kernels/queue.c")!!.readText(),
        PherType.clCode(),
        this::class.java.getResource("/kernels/ant.c")!!.readText()
    ).also {
        // WITH_FALLBACK_PATHFINDING=true
        val opts = mutableListOf("-DMAX_QUEUE_SIZE=$maxQueueSize -DMAX_LOOKUP_ANGLE=${conf.angleDegs}")
        if ( debug ) opts.also { it.add("-DDEBUG") }.also { it.add("-DDEBUG_PATHFIND") } // DEBUG_PATHF DEBUG_QUEUE

        it.build(opts.joinToString(" "))
        d("build ants with $opts")
    }
    private val kernel = prog.createKernel("ant_kernel").also {
        //it.setArg(1, thres)
    }

    /// entities
    private val posBuff = BufferUtils.createByteBuffer(conf.totalCount * FLOAT2_SIZE)
    private val velBuff = BufferUtils.createByteBuffer(conf.totalCount * FLOAT2_SIZE)
    /** 0 - empty, looking for food, 1 - with food */
    /// TODO bitmask
    private val stateBuff = BufferUtils.createByteBuffer(conf.totalCount * BOOL_SIZE)

    private val posCLBuff = ctx.createBuffer(posBuff)
    private val velCLBuff = ctx.createBuffer(velBuff)
    private val stateCLBuff = ctx.createBuffer(stateBuff)
    //private val outPosCLBuff = ctx.createBuffer(posBuff.capacity().toLong())
    //private val outPheromones = ctx.createBuffer(pheromones.size)

    //private val shapeRenderer = ShapeRenderer()

    private var actlast: Long = 0

    fun ejectAntsFromNest() {
        val nest = ground.report.getMedian()
        val nestradius = ground.report.getBoundingBox().let {
            (max(it.width, it.height) / 2) + 2f
        }
        println("Nest = $nest (radius = $nestradius)")
        return ejectAntsFromPoint(nest, nestradius)
    }

    fun ejectAntsFromPoint(center: Vector2, radius: Float) {
        val p = posBuff.asFloatBuffer()
        val v = velBuff.asFloatBuffer()

        val angleDiff: Float = PI.toFloat() * 2 / conf.totalCount
        val tempVector = Vector2(radius, 0f)

        for (i in 0 until conf.totalCount) {
//            p.put(i * 2, random.nextFloat() * w)
//            p.put(i * 2 + 1, random.nextFloat() * h)
            val coords = tempVector + center
            p.put(i * 2, coords.x)
            p.put(i * 2 + 1, coords.y)
            //val vel = Vector2(max_speed, 0f).rotateRad(2*PI.toFloat()*random.nextFloat())
            //v.put( i * 2, vel.x)
            //v.put( i * 2 + 1, vel.y)
            val vel = tempVector.normalized()
            v.put( i * 2, vel.x)
            v.put( i * 2 + 1, vel.y)
            //v.put( i * 2, 0f)
            //v.put( i * 2 + 1, max_speed)
            tempVector.rotateRad(angleDiff)
        }
    }

    override fun act(delta: Float) {
        actlast = measureTimeMillis {
            time += delta
            //val r = (max_speed * delta)
            //val s = ceil(PI * r * r * angle_degs / 360)
            pheromones.act(delta)

            with(kernel) {
                var a = 0
                setArg( a++, time)
                setArg( a++, delta)
                setArg( a++, conf.maxSpeed)
                setArg( a++, width)
                setArg( a++, height)
                setArg( a++, ground.shared.remoteBuff)
                setArg( a++, pheromones.shared.remoteBuff)
                setArg( a++, conf.totalCount)
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
                val ev = enqueueNDRangeKernel(kernel, conf.totalCount.toLong())
                finish()
                /// afterNDRun
                enqueueReadBuffer(from = posCLBuff, to = posBuff)
                enqueueReadBuffer(from = velCLBuff, to = velBuff)
                enqueueReadBuffer(from = stateCLBuff, to = stateBuff)
// if ground does not change?
                enqueueReadBuffer(from = pheromones.shared.remoteBuff, pheromones.shared.buff)
            }

            //debugScanning()
            //debugObstacles()
        }
    }

    fun debugObstacles() {
        val p = posBuff.asFloatBuffer()
        for ( i in 0 until conf.totalCount) {

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
            //this.fire(PauseEvent(pause = true))
            warn("EMPTY SCAN")
            val pos = posBuff.asFloatBuffer()
            val vel = velBuff.asFloatBuffer()
            val c = Vector2(pos[0], pos[1])
            val origin = Vector2(pos[0].toInt() + 0.5f, pos[1].toInt() + 0.5f)
            val v = Vector2(vel[0], vel[1]).setLength(1f)
            val minDot = cos(23.0 * PI / 180f)

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

    fun forEach(block: (pos: Vector2, vel: Vector2, state: AntState) -> Unit) {

        val p = posBuff.asFloatBuffer()
        val v = velBuff.asFloatBuffer()
        val s = stateBuff
        for ( i in 0 until conf.totalCount ) {
            val pos = Vector2(p[2 * i] , p[2 * i + 1])
            val vel = Vector2(v[2 * i] , v[2 * i + 1])
            val st = AntState.fromValue(s[i])
            block(pos, vel, st)
        }
    }

    fun snapshot(): Array<AntSnapshot> {
        val res = mutableListOf<AntSnapshot>()
        forEach { pos, vel, state -> res.add(AntSnapshot(pos, vel, state)) }
        return res.toTypedArray()
    }

    fun set(idx: Int, ant: AntSnapshot) {
        val p = posBuff.asFloatBuffer()
        val v = velBuff.asFloatBuffer()
        p.put(2*idx, ant.pos.x)
        p.put(2*idx+1, ant.pos.y)
        v.put(2*idx, ant.vel.x)
        v.put(2*idx+1, ant.vel.y)
        stateBuff.put(idx, ant.state.value)
    }

}
