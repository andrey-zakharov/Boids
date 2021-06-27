package me.zakharov
import com.badlogic.gdx.Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxGame
import ktx.app.KtxScreen
import me.apemanzilla.ktcl.CLDevice
import me.apemanzilla.ktcl.CLException
import me.apemanzilla.ktcl.cl10.*
import me.zakharov.me.zakharov.MainScreen
import kotlin.time.Duration

val d: (m: Any) -> Unit = ::println
val warn: (m: Any) -> Unit = ::println

class Game(private val device: CLDevice): KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val font by lazy { BitmapFont() }

    private val ctx by lazy { device.createContext() }
    private val cmd by lazy { device.createCommandQueue(ctx) }
    // Kernel(batchND).apply {
    //  load(Resource("saxpy.kernel"))
    //  allocBuffersFromArgs?
    // }
    //
    //val saxpyKernel = Saxpy(ctx, cmd)

    val maxItemsX = device.maxWorkItemSizes[0]
    val maxItemsY = device.maxWorkItemSizes[1]//, maxItemsZ)

    override fun create() {
        val durMs = kotlin.system.measureTimeMillis {
            addScreen(MainScreen(this, ctx, cmd).apply {

            })
            setScreen<MainScreen>()
            super.create()
        }
        d("Created Game: $durMs ms")
    }

//    fun run() {
//        mainView.run()
//    }

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