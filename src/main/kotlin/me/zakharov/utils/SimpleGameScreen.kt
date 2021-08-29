package me.zakharov.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import ktx.app.KtxScreen

fun createScreen(block: SimpleGameScreen.() -> Unit) {

}
open class SimpleGameScreen(
    private val camera: Camera,
    private val batch: Batch?,
    private val input: InputMultiplexer? = Gdx.input.inputProcessor as? InputMultiplexer,
    sceneBlock: Stage.() -> Unit = { }
): KtxScreen {
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)



    private val stage by lazy {
        Stage(FitViewport(Gdx.app.graphics.width.toFloat(), Gdx.app.graphics.height.toFloat(), camera), batch).apply {
            this.sceneBlock()
        }
    }

    override fun dispose() {
        super.dispose()
        mainScope.cancel()
        ioScope.cancel()
    }

    override fun show() {
        super.show()
        input?.apply {
            addProcessor(stage)
        }
    }

    override fun hide() {
        super.hide()
        input?.apply {
            removeProcessor(stage)
        }
    }
}