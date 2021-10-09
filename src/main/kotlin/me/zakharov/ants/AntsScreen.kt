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
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import ktx.app.KtxScreen
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.zakharov.Game
import me.zakharov.Resettable
import me.zakharov.ants.gdx.AntsDrawer
import me.zakharov.ants.gdx.PheromonesDrawer
import me.zakharov.ants.model.Ants
import me.zakharov.ants.model.AntsConfig
import me.zakharov.ants.model.Ground
import me.zakharov.ants.model.Pheromones
import me.zakharov.d
import me.zakharov.events.PauseEvent
import me.zakharov.me.zakharov.ants.gdx.GroundDrawer
import me.zakharov.me.zakharov.ants.gdx.createFromTexture
import me.zakharov.utils.modE
import java.util.*
import kotlin.math.log
import kotlin.math.pow
import kotlin.random.Random

class AntsScreen(
    private val game: Game,
    private val ctx: CLContext,
    val cmd: CLCommandQueue,
    private val input: InputMultiplexer? = Gdx.input.inputProcessor as? InputMultiplexer,

    ) : KtxScreen, Resettable {
    private val screenWidth = Gdx.app.graphics.width
    private val screenHeight = Gdx.app.graphics.height
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /// repository /editor
    private val testGrounds = arrayOf(
        "tex/ground-3rev.png",
        "tex/ground-3.png",
        "tex/ground-test.png",
        "tex/ground.png",
        "tex/ground-2.png",
        "tex/pic.png"
    )
    private var currentTex = 0

    // model
    private var worldWidth = 500//screenWidth / 4// 1 shl 9 // x

    //        set(value) {
//            ground = null
//        }
    private val worldHeight = 500//screenHeight / 5 // 1 shl 9 //
    internal var totalCount: Int = 100
        set(v: Int) {
            //reinit all?
            field = v
        }
    private val antsConfig = AntsConfig(
        worldWidth, worldHeight, totalCount, 10f, 90f
    )


    // 307200 640x480
    private val ground by lazy {
        Ground(ctx, cmd, worldWidth, worldHeight)
            .createFromTexture(Texture(testGrounds[currentTex])).also {
                //println(it.debugString())
            }
    }
    private val pher by lazy { Pheromones(ctx, cmd, ground.width, ground.height) }
    private val ants by lazy {
        Ants(antsConfig, ctx, cmd, ground, pher).also {
            it.ejectAntsFromNest()
        }
    }


    // view
    private val camera by lazy {
//        OrthographicCamera(screenWidth.toFloat(), screenHeight.toFloat()).apply {
//        setToOrtho(false, ground.width.toFloat(), ground.height.toFloat())
        //}
        OrthographicCamera(screenWidth.toFloat(), screenHeight.toFloat()).apply {
            setToOrtho(false, ground.width.toFloat(), ground.height.toFloat())
        }
    }

    private val antsCamera by lazy {
        OrthographicCamera().apply {
            setToOrtho(true, ground.width.toFloat(), ground.height.toFloat())
            //translate(-ground.width * 0.1f, -ground.height * 0.1f)

        }
    }

    private val hudCamera by lazy {
        OrthographicCamera(screenWidth.toFloat(), screenHeight.toFloat()).apply {
            setToOrtho(false, screenWidth.toFloat(), screenHeight.toFloat())
        }
    }

    private val viewport by lazy {
        //FitViewport(camera.viewportWidth, camera.viewportHeight, camera)
        ScreenViewport()
    }

    private val groundDrawer by lazy { GroundDrawer(ground) }
    private val antsDrawer by lazy { AntsDrawer(ants) }
    private val pherDrawer by lazy { PheromonesDrawer(pher) }
    private var pause = false
    private var oneStep = false
    private var zoomFactor = 1f

    interface MouseEventData {
        val event: InputEvent?
        val x: Float
        val y: Float
    }

    val mouseMoveFlow = MutableSharedFlow<MouseEventData>()

    private fun toggleDebug(d: Boolean) {
        scene.isDebugAll = d
        antsScene.isDebugAll = d
    }

    private val scene = Stage(viewport, game.batch).apply {
        d("Creating scene for world size = [${ground.width}, ${ground.height}]")
        //stage
        addListener(object : InputListener() {
            override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                mouseMoveFlow.tryEmit(object : MouseEventData {
                    override val event = event
                    override val x = x
                    override val y = y
                })
                return super.mouseMoved(event, x, y)
            }

            override fun scrolled(event: InputEvent?, x: Float, y: Float, amountX: Float, amountY: Float): Boolean {
                zoomFactor += amountX / 10f;
                //this@apply.viewport.camera.
                return true
            }

            override fun keyUp(event: InputEvent?, keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.SPACE -> pause = !pause
                    Input.Keys.S -> oneStep = true
                    Input.Keys.R -> {
                        pher.reset()
                        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                            ants.reset()
                        }
                    }
                    Input.Keys.P -> pher.print()
                    Input.Keys.D -> toggleDebug(!isDebugAll)
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

        scope.launch {
            mouseMoveFlow.debounce(100).collect {
                //raycast current object under cursor
                //antsScene.viewport.getPickRay()
                println(it)


            }
        }

        addActor(groundDrawer)
        addActor(pherDrawer)


        antsDrawer.addListener {
            when (it) {
                is PauseEvent -> {
                    pause = it.pause
                    true
                }
                else -> false
            }
        }
    }

    private val antsScene = Stage(
        StretchViewport(antsCamera.viewportWidth * 1.2f, antsCamera.viewportHeight * 1.2f, antsCamera), game.batch
    ).apply {
        addActor(antsDrawer.apply { debug = false })
    }

    private val lblInfo = object : Label("fps: ", game.uiSkin) {
        override fun act(delta: Float) {
            super.act(delta)
            setText("fps: ${Gdx.graphics.framesPerSecond}\nframeId: ${Gdx.graphics.frameId}")
        }
    }

    private val hudUI by lazy {
        Stage(FitViewport(hudCamera.viewportWidth, hudCamera.viewportHeight, hudCamera), game.batch).apply {

            addActor(VerticalGroup().run {
                columnLeft()
                right()
                bottom()

                addActor(lblInfo)

                addActor(CheckBox("debug", game.uiSkin).also {
                    it.isChecked = scene.isDebugAll
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            toggleDebug(it.isChecked)
                        }
                    })
                })

                addActor(CheckBox("draw ants", game.uiSkin).also {
                    it.isChecked = antsDrawer.enabled
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            antsDrawer.enabled = it.isChecked
                        }
                    })
                })
                // BOOL template
                addActor(CheckBox("draw empty ants", game.uiSkin).also {
                    it.isChecked = antsDrawer.showEmpty
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            antsDrawer.showEmpty = it.isChecked
                        }
                    })
                })
                addActor(HorizontalGroup().run {
                    left()
                    addActor(Label("nbyte: ", game.uiSkin))
                    addActor(Slider(0f, 1000f, 1f, false, game.uiSkin).run {
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent?, actor: Actor?) {
                                groundDrawer.nbyte = this@run.value.toInt()
                            }
                        })
                        this
                    })
                    this
                })

                addActor(HorizontalGroup().run {
                    left()
                    addActor(Label("div: ", game.uiSkin))
                    addActor(Slider(-2f, 2f, 0.1f, false, game.uiSkin).run {
                        value = log(groundDrawer.div, 10f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent?, actor: Actor?) {
                                groundDrawer.div = 10f.pow(this@run.value)
                            }
                        })
                        this
                    })
                    this
                })
                setFillParent(true)
                this
            })
        }
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
        scope?.cancel("hiding")
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        scene.viewport.update(width, height)
        antsScene.viewport.update(width, height)
        hudUI.viewport.update(width, height)
    }


    override fun render(delta: Float) {
        camera.update()
        game.batch.projectionMatrix = camera.combined
        scene.viewport.apply()

        if (!pause || oneStep) {
            if (oneStep) {
                // clear pher
                //pher.reset()
            }
            if (groundDrawer.nbyte == 0 || Gdx.graphics.frameId % groundDrawer.nbyte == 0L) {
                scene.act()
                antsScene.act()
                oneStep = false
            }
        }


        scene.camera.update()
        scene.batch.projectionMatrix = camera.combined
        scene.viewport.apply()
        scene.draw()

        antsScene.camera.update()
        antsScene.batch.projectionMatrix = antsCamera.combined
        antsScene.viewport.apply()
        antsScene.draw()

        hudCamera.update()
        hudUI.batch.projectionMatrix = hudCamera.combined
        hudUI.viewport.apply(true)
        hudUI.act()
        hudUI.draw()

        //game.batch.end()

//        if (Gdx.input.isTouched) {
//            game.addScreen(GameScreen(game))
//            game.setScreen<GameScreen>()
//            game.removeScreen<MainMenuScreen>()
//            dispose()
//        }
    }

    override fun reset() {
        println("no reset")
    }
}
