package me.zakharov.me.zakharov.replicators.gdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.Actor
import me.zakharov.me.zakharov.replicators.model.MAX_ENERGY
import me.zakharov.me.zakharov.replicators.model.WorldSystem
import me.zakharov.utils.*
import me.zakharov.utils.ByteTextureData
import me.zakharov.utils.FloatTextureData

class WorldDrawer(private val model: WorldSystem) : Actor() {
    private val data by lazy {
        arrayOf(model.light, model.minerals, model.moisture).map {
            FloatTextureData(model.cf.width, model.cf.height, it.buff)
        }
    }
    // obstacles
    private val cells by lazy {
        ByteTextureData(model.cf.width, model.cf.height, model.cells.buff)
    }
    private val tex by lazy {
        data.map {
            Texture(it).apply {
                setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
                load(it)
            }
        }
    }
    private val texCells by lazy {
        Texture(cells).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            load(cells)
        }
    }
    private val shaderProgram = ShaderProgram(
        Gdx.files.internal("shaders/genoms/ground.vert"),
        Gdx.files.internal("shaders/genoms/ground.frag")
    ).apply {
        assert(isCompiled) { "shader compile failed: $log" }
        bind()
        safeSetUniform("max_light", MAX_ENERGY)
    }

    private var time = 0f

    override fun act(delta: Float) {
        super.act(delta)
        time += delta
        //shaderProgram.setUniformf("time", time)
    }
    override fun draw(batch: Batch?, parentAlpha: Float) {
        tex.forEachIndexed { index, texture -> texture.load(data[index]) }
        texCells.load(cells)

        super.draw(batch, parentAlpha)
        batch?.let {
            it.shader = shaderProgram

            texCells.bind(1)
            it.shader.safeSetUniform("u_cells", 1)
            tex[0].bind(0)
            it.shader.safeSetUniform("u_texture", 0)
            it.draw(tex[0], 0f, 0f, stage.width, stage.height)
            it.shader = null

        }
    }
}