package me.zakharov.ants.gdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.Actor
import me.zakharov.ants.model.Pheromones
import me.zakharov.utils.WrappedFloatTextureData

class PheromonesDrawer(pher: Pheromones): Actor() {

    private val shaderProgram = ShaderProgram(
        Gdx.files.internal("shaders/pheromones.vert"),
        Gdx.files.internal("shaders/pheromones.frag")
    ).apply {
        assert(isCompiled) { "shader compile failed: $log" }
    }

    private val wrappedTexData = WrappedFloatTextureData(pher.width, pher.height, pher.m.buff)
    private val glTex = Texture(wrappedTexData).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
    }
    private val glReg = TextureRegion(glTex).also {
        it.setRegion(-0.1f, -0.1f, 1.1f, 1.1f)
    }
    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        batch?.let { b ->
            b.shader = shaderProgram
            glTex.load(wrappedTexData)
            //b.draw(glTex, 0f, 0f, 200f, 200f)
            val d = kotlin.math.min(stage.width, stage.height)
            b.draw(glReg, 0f, 0f, stage.width, stage.height)
            b.shader = null
        }
    }
}
