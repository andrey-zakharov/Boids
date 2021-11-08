package me.zakharov.ants.gdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.Actor
import me.zakharov.Const
import me.zakharov.ants.model.Pheromones
import me.zakharov.utils.FloatTextureData

class PheromonesDrawer(pher: Pheromones): Actor() {
    var debug = 0

    private val shaderProgram = ShaderProgram(
        Gdx.files.internal("shaders/pheromones.vert"),
        Gdx.files.internal("shaders/pheromones.frag")
    ).apply {
        assert(isCompiled) { "shader compile failed: $log" }
    }

    private val data = arrayOf(
        FloatTextureData(pher.width, pher.height, pher.m.buff.apply { position(0) }.slice()),
        FloatTextureData(pher.width, pher.height,
            pher.m.buff.apply {position(pher.width*pher.height* Const.FLOAT_SIZE)}.slice()
        )
    )
    private val tex = data.map {
        Texture(it).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        }
    }
    private val reg = tex.map {
        TextureRegion(it).also {
            it.setRegion(-0.1f, -0.1f, 1.1f, 1.1f)
        }

    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        batch?.let { b ->

            tex.forEachIndexed { index, texture -> texture.load(data[index]) }
            reg.forEachIndexed { index, textureRegion ->
                b.shader = shaderProgram

                textureRegion.texture.bind()
                b.shader.setUniformi("tex_index", index)
                b.draw(textureRegion, 0f, 0f, stage.width, stage.height)
                b.shader = null
            }
        }
    }

}
