package me.zakharov.me.zakharov.ants

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.zakharov.Game
import me.zakharov.ants.gdx.AntsDrawer
import me.zakharov.ants.model.Ants
import me.zakharov.ants.model.AntsConfig
import me.zakharov.ants.model.Ground
import me.zakharov.ants.model.GroundType
import me.zakharov.events.PauseEvent
import me.zakharov.me.zakharov.ants.gdx.GroundDrawer
import me.zakharov.me.zakharov.ants.gdx.createFromTexture
import me.zakharov.utils.SimpleGameScreen

class TestScreen(
        val game: Game,
        val ctx: CLContext,
        val cmd: CLCommandQueue

): SimpleGameScreen(game.camera, batch=game.batch) {
    private val w = Gdx.app.graphics.width
    private val h = Gdx.app.graphics.height
    private val camera = OrthographicCamera().apply {
        setToOrtho(false, w.toFloat(), h.toFloat())
    }
    private val grounds: Array<Ground.() -> Unit> = arrayOf(
        {
            this[0, 0] = GroundType.Nest
            for( i in 0 until height ) {
                this[width/2, i] = GroundType.Obstacle
            }
        }
    )

    private val ground by lazy {
        Ground(ctx, cmd, 10, 10) {
            grounds[0].invoke(this)
        }//.createFromTexture(Texture("tex/ground-2.png"))
    }
    private val ants by lazy { Ants(AntsConfig(ground.width, ground.height), ctx, cmd, ground) }
    private val groundDrawer by lazy { GroundDrawer(ground) }
    private val antsDrawer by lazy { AntsDrawer(ants) }
    private var pause = false

    val scene = Stage(FitViewport(w.toFloat(), h.toFloat(), camera), game.batch).apply {
        println("Test screen scene")
        addActor(groundDrawer)
        addActor(antsDrawer)
        antsDrawer.addListener {
            when(it) {
                is PauseEvent -> {
                    pause = it.pause
                    true
                }
                else -> false
            }
        }
    }

    override fun render(delta: Float) {
        if ( !pause ) {
            scene.act()
        }

        scene.draw()

        scene.batch.begin()
        game.font.draw(scene.batch, "fps ${Gdx.graphics.framesPerSecond}", 0f, h-20f)
        scene.batch.end()
    }
}
