package me.zakharov
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScalingViewport
import ktx.app.KtxGame
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import me.apemanzilla.ktcl.CLDevice
import me.apemanzilla.ktcl.CLException
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.ants.gdx.AntsDrawer
import me.zakharov.ants.model.Ants
import me.zakharov.ants.model.AntsConfig
import me.zakharov.ants.model.Ground
import me.zakharov.ants.model.GroundType
import me.zakharov.me.zakharov.IGameModel
import me.zakharov.me.zakharov.PrimaryGameModel
import me.zakharov.me.zakharov.ants.MainScreen
import me.zakharov.me.zakharov.ants.gdx.GroundDrawer
import me.zakharov.utils.SimpleGameScreen

val d: (m: Any?) -> Unit = ::println
val warn: (m: Any?) -> Unit = ::println

class Game(private val device: CLDevice): KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val uiSkin by lazy { Skin(Gdx.files.internal("skins/comic/comic-ui.json")) }
    val font by lazy { BitmapFont() }
    val model: IGameModel by lazy { PrimaryGameModel() }

    private val ctx by lazy { device.createContext() }
    private val cmd by lazy { device.createCommandQueue(ctx) }
    val maxItemsX = device.maxWorkItemSizes[0]
    val maxItemsY = device.maxWorkItemSizes[1]//, maxItemsZ)

    private val screen1 by lazy { MainScreen(this, ctx, cmd).apply {


    } }

    private val screen2 by lazy { object: SimpleGameScreen(camera = mainCam, batch = batch, input = inputBus) {
        // TODO Ground.fromMatrix, fromPicture ctor
        private val ground by lazy { Ground(ctx, cmd, 5, 5) {
            this[0, 0] = GroundType.Nest
            this[1, 0] = GroundType.Obstacle
            this[2, 0] = GroundType.Food
        } }
        private val ants by lazy { Ants(AntsConfig(5, 5, 1), ctx, cmd, ground) }

        private val groundDrawer by lazy { GroundDrawer(ground) }
        private val antsDrawer by lazy { AntsDrawer(ants, font) }

        private val stage = Stage( ScalingViewport(Scaling.none, w.toFloat(), h.toFloat(), mainCam), batch ).apply {
            addActor(groundDrawer)
            addActor(antsDrawer)
        }




    } }

    // gdx stuff
    private val w by lazy { Gdx.app.graphics.width }
    private val h by lazy { Gdx.app.graphics.height }
    internal val mainCam by lazy { OrthographicCamera().apply {
        setToOrtho(false, w.toFloat(), h.toFloat())
    }}

    private val inputProcessor = object: KtxInputAdapter {
        override fun keyUp(keycode: Int): Boolean = when (keycode) {
            Input.Keys.F1 -> setScreen<MainScreen>() == Unit
            Input.Keys.F2 -> setScreen<SimpleGameScreen>() == Unit
            else -> { println(keycode); super.keyUp(keycode) }
        }
    }

    private val inputBus = InputMultiplexer().apply {
        addProcessor(inputProcessor)
    }

    override fun create() {
        val ver = Gdx.app.graphics.glVersion
        d("GL version: ${ver.majorVersion}.${ver.minorVersion}")
        d("GL renderer: ${ver.vendorString} ${ver.rendererString}")
        //Gdx.graphics.setForegroundFPS(0)
        Gdx.graphics.setVSync(false)
        Gdx.input.inputProcessor = inputBus
        KtxAsync.initiate()
        val durMs = kotlin.system.measureTimeMillis {
            addScreen(screen1)
            addScreen(SimpleGameScreen::class.java, screen2)
            setScreen<MainScreen>()
            super.create()
        }
        d("Created Game: $durMs ms")
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        super.dispose()
    }
}

fun main() {


    try {
        println("Found platforms = ${getPlatforms()}")
        assert(getPlatforms().isNotEmpty())
        val plat = getPlatforms()[0]
        plat.getDefaultDevice()?.let {
            println("extensions ${it.extensions.joinToString("\n - ")}")
            println("image2dMaxDims ${it.image2dMaxHeight}x${it.image2dMaxWidth}")
            println("max compute units ${it.maxComputeUnits}")
            println("max const args ${it.maxConstantArgs}")
            println("workgroup maxes size=${it.maxWorkGroupSize}, item dims=${it.maxWorkItemDimensions}")

            val config = Lwjgl3ApplicationConfiguration().apply {
                setTitle("Ants")
                setWindowedMode(1024, 768)
                setMaximized(true)
                setForegroundFPS(0)
            }
            Lwjgl3Application(Game(it), config).logLevel = Application.LOG_DEBUG
        }

    } catch (e: CLException) {
        System.err.println(e)
        //return 1
        //error("CL exception", e)
    }

    //return 0
}