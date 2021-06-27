package me.zakharov.me.zakharov

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import ktx.app.KtxScreen
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.zakharov.Game
import me.zakharov.Pheromones
import me.zakharov.events.PauseEvent
import java.nio.ByteBuffer
import java.util.*
import kotlin.random.Random

class MainScreen(
        val game: Game,
        val ctx: CLContext,
        val cmd: CLCommandQueue

): KtxScreen {
    private val w = Gdx.app.graphics.width
    private val h = Gdx.app.graphics.height
    private val camera = OrthographicCamera().apply {
        setToOrtho(false, w.toFloat(), h.toFloat())
    }

    private val ground by lazy { Ground(Texture("ground-2.png"), ctx, cmd, 80, 80) }
    private val pher by lazy { Pheromones(ctx, cmd, ground.w, ground.h) }
    private val ants by lazy { Ants(ctx, cmd, ground, pher, game.font) }
    private var pause = false
    private var oneStep = false

    val scene = Stage(FitViewport(w.toFloat(), h.toFloat(), camera), game.batch).apply {
        println("Creating scene")
        addActor(pher)
        addActor(ground)
        addActor(ants)
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
                    else -> return false
                }
                return true
            }
        })

        Gdx.input.inputProcessor = this
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
    }

    override fun render(delta: Float) {
        //camera.update()
        //game.batch.projectionMatrix = camera.combined

        if ( !pause || oneStep) {
            scene.act()
            oneStep = false
        }

        scene.draw()

        scene.batch.begin()
        game.font.draw(scene.batch, "fps ${Gdx.graphics.framesPerSecond}", 0f, h-20f)
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
