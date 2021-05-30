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

class Game(private val device: CLDevice): KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val font by lazy { BitmapFont() }

    private val ctx = device.createContext()
    private val cmd = device.createCommandQueue(ctx)
    val saxpyKernel = Saxpy(ctx, cmd)

    val maxItemsX = device.maxWorkItemSizes[0]
    val maxItemsY = device.maxWorkItemSizes[1]//, maxItemsZ)

    override fun create() {
        addScreen(MainScreen(this, ctx, cmd))
        setScreen<MainScreen>()
        super.create()
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
        println(getPlatforms())
        assert(getPlatforms().isNotEmpty())
        val plat = getPlatforms()[0]
        plat.getDefaultDevice()?.let {
            println(it.extensions)
            println(it.image2dMaxHeight)
            println(it.image2dMaxWidth)
            println(it.maxComputeUnits)

            println(it.maxConstantArgs)
            println(it.maxWorkGroupSize)
            println(it.maxWorkItemDimensions)

            val config = Lwjgl3ApplicationConfiguration().apply {
                setTitle("Ants")
                setWindowedMode(1024, 768)
            }
            Lwjgl3Application(Game(it), config).logLevel = Application.LOG_DEBUG
        }

    } catch (e: CLException) {
        println(e)
        //error("CL exception", e)
    }


}