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
import me.zakharov.events.PauseEvent
import me.zakharov.me.zakharov.ants.gdx.GroundDrawer
import me.zakharov.me.zakharov.ants.gdx.createFromTexture
import me.zakharov.utils.SimpleGameScreen

class TestScreen(
        val game: Game,
        val ctx: CLContext,
        val cmd: CLCommandQueue

): SimpleGameScreen(game.mainCam, game.batch) {
    private val w = Gdx.app.graphics.width
    private val h = Gdx.app.graphics.height
    private val camera = OrthographicCamera().apply {
        setToOrtho(false, w.toFloat(), h.toFloat())
    }

    private val ground by lazy { Ground(ctx, cmd, 300, 200).createFromTexture(Texture("tex/ground-2.png")) }
    private val ants by lazy { Ants(AntsConfig(ground.width, ground.height), ctx, cmd, ground) }
    private val groundDrawer by lazy { GroundDrawer(ground) }
    private val antsDrawer by lazy { AntsDrawer(ants, game.font ) }
    private var pause = false

    val scene = Stage(FitViewport(w.toFloat(), h.toFloat(), camera), game.batch).apply {
        println("Creating scene")
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

    private val pixmap = Pixmap(w, h, Pixmap.Format.RGB888).apply {
        filter = Pixmap.Filter.NearestNeighbour
    }

    private var tex = Texture(pixmap).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
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
