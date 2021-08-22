package me.zakharov

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlinx.coroutines.*
import ktx.app.KtxScreen
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.zakharov.ants.gdx.AntsDrawer
import me.zakharov.ants.model.Ants
import me.zakharov.ants.model.AntsConfig
import me.zakharov.ants.model.Ground
import me.zakharov.events.PauseEvent
import me.zakharov.me.zakharov.ants.gdx.GroundDrawer
import me.zakharov.me.zakharov.ants.gdx.createFromTexture
import java.util.*
import kotlin.math.log
import kotlin.math.pow
import kotlin.random.Random

class MainScreen(
        val game: Game,
        val ctx: CLContext,
        val cmd: CLCommandQueue,
        val input: InputMultiplexer? = Gdx.input.inputProcessor as? InputMultiplexer,

): KtxScreen {
    private val w = Gdx.app.graphics.width
    private val h = Gdx.app.graphics.height
    private val camera = OrthographicCamera().apply {
        setToOrtho(false, w.toFloat(), h.toFloat())
    }

    private val ground by lazy { Ground(ctx, cmd, w/4, h/4)/* {
        var type = 0
        val vals = GroundType.values()
        for (j in 0 until height ) {
            for (i in 0 until width ) {
                this[i, j] = vals[type]
                type = (type + 1) % vals.size
            }
        }

    }*/.createFromTexture( Texture(
//        "tex/ground-2.png"
            "tex/ground-test.png"
//        "tex/ground.png"
//        "tex/pic.png"
        ) )
    }
    private val pher by lazy { Pheromones(ctx, cmd, ground.width, ground.height) }
    private val ants by lazy { Ants(AntsConfig(ground.width, ground.height,100), ctx, cmd, ground, pher) }
    private val groundDrawer by lazy { GroundDrawer(ground) }
    private val antsDrawer by lazy { AntsDrawer(ants, game.font) }
    private var pause = false
    private var oneStep = false

    private val scene = Stage(FitViewport(w.toFloat(), h.toFloat(), camera), game.batch).apply {
        println("Creating scene")
        //stage
        addListener( object: InputListener() {
            override fun keyUp(event: InputEvent?, keycode: Int): Boolean {
                when(keycode) {
                    Input.Keys.SPACE -> pause = !pause
                    Input.Keys.S -> oneStep = true
                    Input.Keys.R -> pher.reset()
                    Input.Keys.P -> pher.print()
                    Input.Keys.D -> { this@apply.isDebugAll = !this@apply.isDebugAll }
                    else -> return false
                }
                return true
            }
        })
        addActor(groundDrawer)
        addActor(antsDrawer.apply { debug = false })
        antsDrawer.addListener {
            when(it) {
                is PauseEvent -> {
                    pause = it.pause
                    true
                }
                else -> false
            }
        }
        addActor( VerticalGroup().run {
            addActor( Slider( 0f, 7f, 1f, false, game.uiSkin ).run {
                addListener(object: ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        groundDrawer.nbyte = this@run.value.toInt()
                    }
                })
                this
            })
            addActor(Label("tested", game.uiSkin))
            addActor(Slider(-2f, 2f, 0.1f, false, game.uiSkin ).run {
                value = log(groundDrawer.div, 10f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        groundDrawer.div = 10f.pow(this@run.value)
                    }
                })
                this
            })
            setFillParent(true)
            this
        })
    }

    val random = Random(Calendar.getInstance().timeInMillis)

    override fun show() {
        input?.apply {
            addProcessor(scene)
        }
    }

    override fun hide() {
        input?.apply {
            removeProcessor(scene)
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /* works slower
    init {
        scope.launch {
            while(isActive) {
                if ( !pause || oneStep) {
                    if ( oneStep ) {
                        // clear pher
                        pher.reset()
                    }
                    scene.act()
                    oneStep = false
                }
                yield()
            }
        }
    }*/

    override fun render(delta: Float) {
        //camera.update()
        //game.batch.projectionMatrix = camera.combined

        if ( !pause || oneStep) {
            if ( oneStep ) {
                // clear pher
                //pher.reset()
            }
            scene.act()
            oneStep = false
        }

        scene.draw()

        scene.batch.begin()
        game.font.draw(scene.batch, "fps: ${Gdx.graphics.framesPerSecond}", 0f, h-20f)
        game.font.draw(scene.batch, "debug: ${scene.isDebugAll}", 0f, h-35f)
        game.font.draw(scene.batch, "div: ${groundDrawer.div}", 0f, h-50f)
        scene.batch.end()
        //game.batch.end()

//        if (Gdx.input.isTouched) {
//            game.addScreen(GameScreen(game))
//            game.setScreen<GameScreen>()
//            game.removeScreen<MainMenuScreen>()
//            dispose()
//        }
    }
}
