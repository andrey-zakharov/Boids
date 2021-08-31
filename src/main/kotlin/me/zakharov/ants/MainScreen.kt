package me.zakharov.me.zakharov.ants

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlinx.coroutines.*
import ktx.app.KtxScreen
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.zakharov.Game
import me.zakharov.ants.gdx.AntsDrawer
import me.zakharov.ants.gdx.PheromonesDrawer
import me.zakharov.ants.model.Ants
import me.zakharov.ants.model.AntsConfig
import me.zakharov.ants.model.Ground
import me.zakharov.ants.model.Pheromones
import me.zakharov.events.PauseEvent
import me.zakharov.me.zakharov.ants.gdx.GroundDrawer
import me.zakharov.me.zakharov.ants.gdx.createFromTexture
import me.zakharov.utils.modE
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
    private val screenWidth = Gdx.app.graphics.width
    private val screenHeight = Gdx.app.graphics.height



    private val testGrounds = arrayOf(
        "tex/ground-test.png",
        "tex/ground.png",
        "tex/ground-2.png",
        "tex/pic.png"
    )
    private var currentTex = 0

    // model
    private val worldWidth = 512
    private val worldHeight = 512
    private val antsConfig = AntsConfig(
        worldWidth, worldHeight,1000, 7f, 90f
    )


    // 307200 640x480
    private val ground by lazy { Ground(ctx, cmd, worldWidth, worldHeight)
        .createFromTexture( Texture(testGrounds[currentTex]) ).also {
            //println(it.debugString())
        }
    }
    private val pher by lazy { Pheromones(ctx, cmd, ground.width, ground.height) }
    private val ants by lazy { Ants(antsConfig, ctx, cmd, ground, pher).also {
        it.ejectAntsFromNest()
    } }


    // view
    private val camera by lazy {
        OrthographicCamera(screenWidth.toFloat(), screenHeight.toFloat()).apply {
        setToOrtho(false, ground.width.toFloat(), ground.height.toFloat())
    }}

    private val antsCamera by lazy {
        OrthographicCamera(screenWidth.toFloat(), screenHeight.toFloat()).apply {
            setToOrtho(true, ground.width.toFloat(), ground.height.toFloat())
    }}

    private val hudCamera by lazy {
        OrthographicCamera(screenWidth.toFloat(), screenHeight.toFloat()).apply {
        setToOrtho(false, screenWidth.toFloat(), screenHeight.toFloat())
    }}

    private val groundDrawer by lazy { GroundDrawer(ground) }
    private val antsDrawer by lazy { AntsDrawer(ants, game.font) }
    private val pherDrawer by lazy { PheromonesDrawer(pher) }
    private var pause = false
    private var oneStep = false
    private var zoomFactor = 1f

    private val scene = Stage(FitViewport(camera.viewportWidth, camera.viewportHeight, camera), game.batch).apply {
        println("Creating scene")
        //stage
        addListener( object: InputListener() {
            override fun scrolled(event: InputEvent?, x: Float, y: Float, amountX: Float, amountY: Float): Boolean {
                zoomFactor += amountX / 10f;
                //this@apply.viewport.camera.
                return true
            }
            override fun keyUp(event: InputEvent?, keycode: Int): Boolean {
                when(keycode) {
                    Input.Keys.SPACE -> pause = !pause
                    Input.Keys.S -> oneStep = true
                    Input.Keys.R -> pher.reset()
                    Input.Keys.P -> pher.print()
                    Input.Keys.D -> { this@apply.isDebugAll = !this@apply.isDebugAll }
                    Input.Keys.LEFT -> {
                        currentTex -= 1
                        currentTex = currentTex.modE(testGrounds.size)
                        ground.createFromTexture(Texture(testGrounds[currentTex]))
                        pher.reset()
                    }
                    Input.Keys.RIGHT -> {
                        currentTex += 1
                        currentTex = currentTex.modE(testGrounds.size)
                        ground.createFromTexture(Texture(testGrounds[currentTex]))
                        pher.reset()
                    }
                    else -> return false
                }
                return true
            }
        })

        addActor(groundDrawer)
        addActor(pherDrawer)


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

    private val antsScene = Stage(
        FitViewport(antsCamera.viewportWidth, antsCamera.viewportHeight, antsCamera), game.batch
    ).apply {
        addActor(antsDrawer.apply { debug = false })
    }

    private val hudUI = Stage(FitViewport(hudCamera.viewportWidth, hudCamera.viewportHeight, hudCamera), game.batch).apply {

        addActor( VerticalGroup().run {
            addActor(CheckBox("draw ants", game.uiSkin).also {
                it.isChecked = antsDrawer.enabled
                addListener(object: ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        antsDrawer.enabled = it.isChecked
                    }
                })
            })
            addActor( Slider( 0f, 7f, 1f, false, game.uiSkin ).run {
                addListener(object: ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        groundDrawer.nbyte = this@run.value.toInt()
                    }
                })
                this
            })

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
            addProcessor(hudUI)
        }
    }

    override fun hide() {
        input?.apply {
            removeProcessor(scene)
            removeProcessor(hudUI)
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        scene.viewport.update(width, height)
        hudUI.viewport.update(width, height)
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
        camera.update()
        game.batch.projectionMatrix = camera.combined
        scene.viewport.apply()


        if ( !pause || oneStep) {
            if ( oneStep ) {
                // clear pher
                //pher.reset()
            }
            scene.act()
            oneStep = false
        }


        camera.update()
        scene.batch.projectionMatrix = camera.combined
        scene.viewport.apply()
        scene.draw()

        antsCamera.update()
        antsScene.batch.projectionMatrix = antsCamera.combined
        antsScene.viewport.apply()
        antsScene.act()
        antsScene.draw()


        hudCamera.update()
        hudUI.batch.projectionMatrix = hudCamera.combined
        hudUI.viewport.apply(true)
        hudUI.act()
        hudUI.draw()
        scene.batch.begin()
        game.font.draw(scene.batch, "fps: ${Gdx.graphics.framesPerSecond}", 0f, 20f)
        game.font.draw(scene.batch, "debug: ${scene.isDebugAll}", 0f, 35f)
        game.font.draw(scene.batch, "div: ${groundDrawer.div}", 0f, 50f)
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
