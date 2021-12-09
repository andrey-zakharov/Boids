package me.zakharov.me.zakharov.replicators.gdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import me.zakharov.me.zakharov.replicators.model.BacteriaSystem
import me.zakharov.utils.*
import me.zakharov.utils.FloatTextureData
import me.zakharov.utils.IntTextureData
import java.nio.FloatBuffer
import java.nio.IntBuffer


class BacteriaDrawer(val model: BacteriaSystem) : Actor() {

    var uniformSelectedX: Int = 0
    var uniformSelectedY: Int = 0

    var hovered = Vector2(Vector2.Zero)
    private val data by lazy { arrayOf(
        IntTextureData(model.world.cf.width, model.world.cf.height, model.field.buff), // bacteria_field
        FloatTextureData(model.world.cf.width, model.world.cf.height, model.age._buff), // age
        FloatTextureData(model.world.cf.width, model.world.cf.height, model.energy._buff), // energy
    )}

    private val tex by lazy {
        data.map {
            Texture(it).apply {
                setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            }
        }
    }

    private val shaderProgram = ShaderProgram(
        Gdx.files.internal("shaders/genoms/bacteria.vert").readString(),
        Gdx.files.internal("shaders/genoms/bacteria.frag").readString()
            .replace("_-MAX_ITEMS-_", model.max.toString())
    ).apply {
        assert(isCompiled) { "shader compile failed: $log" }
        bind()
    }

    private var time = 0f

    override fun act(delta: Float) {
        super.act(delta)
        time += delta
        //shaderProgram.setUniformf("time", time)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        tex.forEachIndexed { index, texture -> texture.load(data[index]) }
        super.draw(batch, parentAlpha)
        batch?.let {
            it.shader = shaderProgram

            tex[2].bind(2)
            it.shader.safeSetUniform("u_energy", 2)

            tex[1].bind(1)
            it.shader.safeSetUniform("u_age", 1)

            tex[0].bind(0)
            it.shader.safeSetUniform("u_texture", 0)

            it.shader.safeSetUniform("time", time)


            it.shader.safeSetUniform("u_selected", uniformSelectedX, uniformSelectedY)
            it.shader.safeSetUniform("u_hovered", hovered.x, hovered.y)
            it.shader.safeSetUniform("max_index", model.current_idx)
            it.shader.safeSetUniform("max_age", model.cf.maxAge)
            it.shader.safeSetUniform("u_resolution", model.world.cf.width, model.world.cf.height)

            it.draw(tex[0], 0f, 0f, stage.width, stage.height)
            //it.draw(texAge, 5f, stage.height/2, model.max.toFloat(), 1f)
            it.shader = null

            //it.draw(texAge2, 0f, 0f, stage.width/2, stage.height/2)
            //test.draw(it)
            //it.draw(test2, 10f + kotlin.math.sin(time), 10f + kotlin.math.cos(time))
        }
    }



}
