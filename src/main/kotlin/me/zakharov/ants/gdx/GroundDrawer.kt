package me.zakharov.me.zakharov.ants.gdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.Actor
import ktx.async.RenderingScope
import me.zakharov.ants.model.Ground
import me.zakharov.ants.model.GroundType
import me.zakharov.d
import me.zakharov.utils.WrappedByteTextureData
import me.zakharov.utils.safeSetUniform

class GroundDrawer(private val model: Ground) : Actor() {

    private val scope by lazy { RenderingScope() }
    var div = 1f // uniform
        set(v) {
            shaderProgram.bind()
            shaderProgram.safeSetUniform("div", v)
            field = v
        }

    var nbyte = 0
        set(v) {
            shaderProgram.bind()
            shaderProgram.safeSetUniform("nbyte", nbyte)
            field = v
        }

    var drawGround = true
    private val wrappedTexData by lazy {
        WrappedByteTextureData(model.width, model.height, model.shared.buff)
    }
    private val glTex by lazy { Texture(wrappedTexData).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        load(wrappedTexData)
    } }
    private val glReg = TextureRegion(glTex).also {
        it.setRegion(-0.1f, -0.1f, 1.1f, 1.1f)
    }
    private val shaderProgram = ShaderProgram(
        Gdx.files.internal("shaders/ground.vert"),
        Gdx.files.internal("shaders/ground.frag")
    ).apply {
        assert(isCompiled) { "shader compile failed: $log" }
        bind()
        safeSetUniform("div", div)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
//        shaderProgram.setUniformf("div", div)
        batch?.let {
            it.shader = shaderProgram
            glTex.load(wrappedTexData)
            it.draw(glReg, 0f, 0f, stage.width, stage.height)
            it.shader = null
        }
    }

}

/// this is GDX addition for model Ground
fun Ground.createFromTexture(groundTexture: Texture): Ground {
    report.reset()

    groundTexture.textureData.prepare()
    val px = groundTexture.textureData.consumePixmap()
    val cpx = Pixmap(width, height, Pixmap.Format.RGBA8888).apply {
        filter = Pixmap.Filter.NearestNeighbour
        blending = Pixmap.Blending.None
        drawPixmap(px, 0, 0, px.width, px.height, 0, 0, width, height)
    }

    val c = Color()
    for (y in 0 until height) {
        for ( x in 0 until width) {
            c.set(cpx.getPixel(x, y))
            val groundType = when {
                (c.g > c.r && c.g > c.b) -> GroundType.Food
                (c.r == c.g && c.r == c.b && c.r > 0) -> GroundType.Nest
                (c.r > c.g && c.r > c.b) -> GroundType.Obstacle
                else -> GroundType.Empty
            }

            this[x, y] = groundType
            //cpx.drawPixel(x, y, Color.rgba8888(groundType.color))
        }
    }

    groundTexture.textureData.disposePixmap()
    d(report)
    update(true)
    return this
}
