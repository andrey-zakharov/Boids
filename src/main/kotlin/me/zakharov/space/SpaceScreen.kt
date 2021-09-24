package me.zakharov.me.zakharov.space

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.viewport.FitViewport
import ktx.app.KtxScreen
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.Game
import me.zakharov.d
import me.zakharov.utils.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.PI
import kotlin.system.measureNanoTime

class SpaceScreen(
    private val game: Game,
    private val ctx: CLContext,
    val cmd: CLCommandQueue,
    private val input: InputMultiplexer? = Gdx.input.inputProcessor as? InputMultiplexer,
) : KtxScreen {
    private val screenWidth get() = Gdx.app.graphics.width
    private val screenHeight get() = Gdx.app.graphics.height
    private val viewport by lazy { FitViewport(screenWidth.toFloat(), screenHeight.toFloat()) }



    private val model by lazy { Space(Space.Config(), ctx, cmd).also {
        it.fillRandom()
    } }

    private val camera by lazy { PerspectiveCamera(65f, screenWidth.toFloat(), screenHeight.toFloat()).also {
        it.translate(
            com.badlogic.gdx.math.Vector3(
                model.conf.maxDims.x / 4,
                model.conf.maxDims.y / 4,
                model.conf.maxDims.z / 4
            )
        )
        it.lookAt(Vector3.Zero)
    } }
    private val ui by lazy {
        Stage(viewport, game.batch).run {
            addActor(VerticalGroup().apply {
                top()
                grow()
                addActor(TextButton("add some", game.uiSkin).also {
                    it.addListener { ev ->
                        this@SpaceScreen.onAddSome(ev)
                        true
                    }
                })
            })
            this
        }
    }

    fun onAddSome(e: Event) {
        println("onAddSome $e")
    }

    override fun show() {
        super.show()
        input?.apply {
            addProcessor(ui)
        }
    }

    override fun hide() {
        super.hide()
        input?.apply {
            removeProcessor(ui)
        }
    }

    private val tex by lazy { Texture("tex/body.png") }

    class Stepper(initial: Float, val delta: Float): Iterator<Float> {
        private var value = initial
        override fun hasNext() = true
        override fun next(): Float {
            val ret = value
            value += delta
            return ret
        }

        operator fun inc() = Stepper(next(), delta)
    }

    override fun render(delta: Float) {
        val modelAct = measureNanoTime {
            model.act(delta)
        }
        val uiDraw = measureNanoTime {
            ui.camera.update()
            ui.batch.projectionMatrix = ui.camera.combined
            ui.viewport.apply(true)
            ui.draw()
        }

        val modelDraw = measureNanoTime {
            camera.update()
            game.batch.projectionMatrix = camera.combined

            game.batch.begin()
            model.foreach { b ->
                game.batch.draw(tex, b.pos.x, b.pos.y)
            }
            game.batch.end()

        }

        // custom drawings
        ui.batch.begin()
        val i = Stepper(20f, 15f)
        game.font.draw(ui.batch, delta.toString(), 0f, i.next())
        game.font.draw(ui.batch, "modelAct:  % 10d".format(modelAct), 0f, i.next())
        game.font.draw(ui.batch, "uiDraw:    % 10d".format(uiDraw), 0f, i.next())
        game.font.draw(ui.batch, "modelDraw: % 10d".format(modelDraw), 0f, i.next())
        ui.batch.end()
        super.render(delta)
    }
}

class Space(val conf: Config,
            private val ctx: CLContext,
            private val cmd: CLCommandQueue,) : IHeadlessActor {

    var debug = false

    data class Config(
        val maxCount: Int = 1000,
        val maxDims: Vector3 = Vector3(10e6f, 10e6f, 10e6f),
        val maxMass: Float = 10f,
        val maxVelocity: Float = 100f,
    )


    fun foreach(block: (b: me.zakharov.space.model.Body) -> Unit) {
        for ( i in 0 until conf.maxCount ) {
            // TBD pool?
            block( me.zakharov.space.model.Body(
                kernelBuffers[0].buff.asFloatBuffer()[i],
                Vector3(
                    kernelBuffers[1].buff.asFloatBuffer()[3*i],
                    kernelBuffers[1].buff.asFloatBuffer()[3*i+1],
                    kernelBuffers[1].buff.asFloatBuffer()[3*i+2]
                ),
                Vector3(
                    kernelBuffers[2].buff.asFloatBuffer()[3*i],
                    kernelBuffers[2].buff.asFloatBuffer()[3*i+1],
                    kernelBuffers[2].buff.asFloatBuffer()[3*i+2]
                )
            ))
        }
    }

    internal fun unpack(b: ByteBuffer, idx: Int): Vector3 {
        val f = b.asFloatBuffer()
        return Vector3(f[3*idx], f[3*idx+1], f[3*idx+2])
    }
    private val kernelBuffers = arrayOf(
        ctx.createSharedFloatArray(conf.maxCount),// mass
        ctx.createSharedFloat3Array(conf.maxCount),// pos
        ctx.createSharedFloat3Array(conf.maxCount),// vel
    )
    private val prog = ctx.createProgramWithSource(
        this::class.java.getResource("/kernels/space.c")!!.readText(),
    ).also {
        // WITH_FALLBACK_PATHFINDING=true
        val opts = mutableListOf<String>()
        if ( debug ) {
            opts.add("DEBUG")
        }

//        opts.addAll(conf.clopts)

        it.build(opts.map { "-D$it" }.joinToString(" "))
        d("build space with $opts")
    }
    private val kernel = prog.createKernel("space_kernel").also {
        it.setArg(1, conf.maxCount)
        for (i in 2 .. 4) {
            it.setArg(i, kernelBuffers[i-2].remoteBuff)
        }
    }

    // currentCap
    override fun act(delta: Float) {
        //collect
        with( kernel ) {
            setArg(0, delta )
            setArg(1, conf.maxCount)
            for (i in 2 .. 4) {
                setArg(i, kernelBuffers[i-2].remoteBuff)
            }

        }

        with(cmd) {
            kernelBuffers.forEach { enqueueWrite(it) }
            enqueueNDRangeKernel(kernel, conf.maxCount.toLong(), 0)
            kernelBuffers.forEach { enqueueRead(it) }

        }


    }

    private val random = Random(Calendar.getInstance().timeInMillis)

    fun fillRandom() {
        val bb = ByteArray(1)
        for( i in 0 until conf.maxCount ) {
            //mass
            with(kernelBuffers[0].buff.asFloatBuffer()) {
                put(i, random.nextFloat() * conf.maxMass)
            }
            //pos
            with(kernelBuffers[1].buff.asFloatBuffer()) { // asVector3Buffer
                put(3 * i, conf.maxDims.x * (random.nextFloat() - .5f))
                put(3 * i + 1, conf.maxDims.y * (random.nextFloat() - .5f))
                put(3 * i + 2, conf.maxDims.z * (random.nextFloat() - .5f))
            }

            //vel
            with(kernelBuffers[2].buff.asFloatBuffer()) {
                val v = Vector3(conf.maxVelocity, 0f, 0f)
                    .rotateRad(Vector3.Y, 2 * PI.toFloat() * random.nextFloat())
                    .rotateRad(Vector3.Z, 2 * PI.toFloat() * random.nextFloat())

                put(3 * i, v.x)
                put(3 * i + 1, v.y)
                put(3 * i + 2, v.z)
            }
        }
    }
}