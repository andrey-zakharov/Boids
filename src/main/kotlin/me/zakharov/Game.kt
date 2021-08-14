package me.zakharov
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxGame
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import me.apemanzilla.ktcl.CLDevice
import me.apemanzilla.ktcl.CLException
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.me.zakharov.*
import me.zakharov.me.zakharov.utils.SimpleGameScreen

val d: (m: Any) -> Unit = ::println
val warn: (m: Any) -> Unit = ::println

class Game(private val device: CLDevice): KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
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
        private val ground by lazy { Ground(Texture("tex/ground-test.png"), ctx, cmd, 5, 5) }
        private val ants by lazy { Ants(AntsConfig(5, 5,font, 1), ctx, cmd, ground) }




    } }

    // gdx stuff
    private val w = Gdx.app.graphics.width
    private val h = Gdx.app.graphics.height
    internal val mainCam by lazy { OrthographicCamera().apply {
        setToOrtho(false, w.toFloat(), h.toFloat())
    }}

    private val inputProcessor = object: KtxInputAdapter {
        override fun keyUp(keycode: Int): Boolean = when (keycode) {
            Input.Keys.F1 -> setScreen<MainScreen>() == Unit
            Input.Keys.F2 -> setScreen<TestScreen>() == Unit
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
        val durMs = kotlin.system.measureTimeMillis {
            addScreen(screen1)
            addScreen(screen2)
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