package me.zakharov.me.zakharov.replicators.gdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import me.zakharov.replicators.model.BacteriaSystem
import me.zakharov.utils.*

//@CL
enum class ShowMode(override val bit: Long) : Flags {
    Age(1 shl 0),
    Energy(1 shl 1),
    Balance(1 shl 2),
    Gen(1 shl 3),
}
// view stuff
enum class BacteriaDrawerOptions(val label: String, val flag: ShowMode) {
    A("age channel", ShowMode.Age),
    E("energy channel", ShowMode.Energy),
    B("balance", ShowMode.Balance),
    G("gen channel", ShowMode.Gen),
}

interface Controllable {
    val controls: Sequence<Actor>
}

class BacteriaDrawer(val model: BacteriaSystem,
                     private val skin: com.badlogic.gdx.scenes.scene2d.ui.Skin) : Actor(), Controllable {

    override val controls: Sequence<Actor>
        get() = BacteriaDrawerOptions.values().map { opt ->
            CheckBox(opt.label, skin).also {
                it.isChecked = showLayer.hasFlag(opt.flag)
                it.addListener(object: ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        if ( it.isChecked ) {
                            showLayer += opt.flag
                        } else {
                            showLayer -= opt.flag
                        }
                    }
                })
            }
        }.asSequence()
    var uniformSelectedX: Int = 0
    var uniformSelectedY: Int = 0

    var showLayer: BitMask = ShowMode.Age + ShowMode.Energy + ShowMode.Balance

    var hovered = Vector2(Vector2.Zero)
    private val data by lazy { arrayOf(
        IntTextureData(model.world.cf.width, model.world.cf.height, model.field.buff), // bacteria_field
        FloatTextureData(model.world.cf.width, model.world.cf.height, model.age._buff), // age
        FloatTextureData(model.world.cf.width, model.world.cf.height, model.energy._buff), // energy
        ByteTextureData(model.cf.genLen, model.world.cf.totalCells, model.gen._buff), // gen
    )}

    private val tex by lazy {
        data.map {
            Texture(it).apply {
                setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            }
        }
    }
    private val testDrawer by lazy {
        BufferDrawer(data[3])
    }

    private val test2Drawer by lazy { BufferDrawer(data[0]) }
    private val test3Drawer by lazy { BufferDrawer(data[1]) }

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

            tex[3].bind(3)
            it.shader.safeSetUniform("u_gen", 3)

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
            it.shader.safeSetUniform("u_showLayer", showLayer.value.toInt())
            it.shader.safeSetUniform("gen_length", model.cf.genLen)
            it.shader.safeSetUniform("w", model.world.cf.width)
            it.shader.safeSetUniform("h", model.world.cf.height)

            it.draw(tex[0], 0f, 0f, stage.width, stage.height)
            //it.draw(texAge, 5f, stage.height/2, model.max.toFloat(), 1f)
            it.shader = null

//
//            it.draw(testDrawer, 10f, 10f)
//            it.draw(test2Drawer, 20f, 20f)
//            it.draw(test3Drawer, 150f, 150f)

            //it.draw(texAge2, 0f, 0f, stage.width/2, stage.height/2)
            //test.draw(it)
            //it.draw(test2, 10f + kotlin.math.sin(time), 10f + kotlin.math.cos(time))
        }
    }



}
