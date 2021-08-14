package me.zakharov.me.zakharov

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ktx.app.KtxScreen
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.zakharov.Game
import me.zakharov.Pheromones
import me.zakharov.events.PauseEvent
import java.util.*
import kotlin.properties.Delegates
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

    private val ground by lazy { Ground(Texture(
//        "tex/ground-2.png"
        "tex/ground-test.png"
//        "tex/ground.png"
//        "tex/pic.png"
    ), ctx, cmd, w / 20, h / 20) }
    private val pher by lazy { Pheromones(ctx, cmd, ground.w, ground.h) }
    private val ants by lazy { Ants(AntsConfig(ground.w, ground.h, game.font, 1), ctx, cmd, ground, pher) }
    private var pause = false
    private var oneStep = false

    private val scene = Stage(FitViewport(w.toFloat(), h.toFloat(), camera), game.batch).apply {
        println("Creating scene")
        addActor(ground)
        addActor(ants.apply { debug = false })
        ants.addListener {
            when(it) {
                is PauseEvent -> {
                    pause = it.pause
                    true
                }
                else -> false
            }
        }
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

        //Gdx.input.inputProcessor = this
    }

    val random = Random(Calendar.getInstance().timeInMillis)

    private val pixmap = Pixmap(w, h, Pixmap.Format.RGB888).apply {
        filter = Pixmap.Filter.NearestNeighbour
    }

    private var tex = Texture(pixmap).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    override fun show() {
        pixmap.pixels.apply {
            for( i in 0 until capacity()) {
                put(i, random.nextInt().toByte())
            }
        }
        pixmap.setColor(Color.RED)
        pixmap.drawCircle( pixmap.width / 2, pixmap.height / 2, 100)
        pixmap.setColor(Color.BLUE)
        pixmap.drawRectangle(0, 0, pixmap.width, pixmap.height)
        tex = Texture(pixmap)
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
