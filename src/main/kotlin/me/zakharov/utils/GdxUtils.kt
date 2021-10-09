package me.zakharov.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.TextureData
import com.badlogic.gdx.graphics.glutils.FloatTextureData
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import java.nio.ByteBuffer

internal class WrappedFloat3dTextureData(val w: Int, val h: Int, val z: Int, private val buff: ByteBuffer) :
    TextureData
{
    override fun getType() = TextureData.TextureDataType.Custom
    private var _isPrepared = false
    override fun isPrepared(): Boolean = _isPrepared
    override fun prepare() {
        if (isPrepared) throw GdxRuntimeException("Already prepared")
        _isPrepared = true
    }
    override fun consumePixmap(): Pixmap? {
        throw GdxRuntimeException("This TextureData implementation does not return a Pixmap")
    }
    override fun disposePixmap(): Boolean {
        throw GdxRuntimeException("This TextureData implementation does not return a Pixmap")
    }

    override fun consumeCustomData(target: Int) {
        Gdx.gl30.glTexImage3D(target, 0, GL30.GL_R32F,
            w, h, z, 0,
            GL30.GL_RED, GL20.GL_FLOAT, buff
        )
    }

    override fun getWidth() = w
    override fun getHeight() = h
    override fun getFormat() = Pixmap.Format.RGBA8888 // it's not true, but FloatTextureData.getFormat() isn't used anywhere
    override fun useMipMaps() = false
    override fun isManaged() = false
}

internal class WrappedFloatTextureData(w: Int, h: Int, private val buff: ByteBuffer)
    : FloatTextureData(w, h, GL30.GL_R32F, GL30.GL_RED, 0, false) {
    override fun consumeCustomData(target: Int) {
        Gdx.gl.glTexImage2D(target, 0, GL30.GL_R32F, width, height, 0, GL30.GL_RED, GL20.GL_FLOAT, buff)
    }
}


internal class WrappedByteTextureData(val w: Int, val h: Int, private val buff: ByteBuffer) : TextureData {
    private var isPrepared = false

    override fun getType() = TextureData.TextureDataType.Custom
    override fun isPrepared() = isPrepared

    override fun prepare() {
        if (isPrepared) throw GdxRuntimeException("Already prepared")
        isPrepared = true
    }


    //: FloatTextureData(w, h, GL30.GL_R8, GL30.GL_RED, 0, false) {
    override fun consumeCustomData(target: Int) {
        Gdx.gl.glTexImage2D(target, 0, GL30.GL_R8I, width, height, 0, GL30.GL_RED_INTEGER, GL20.GL_BYTE, buff)
    }

    override fun getWidth() = w
    override fun getHeight() = h
    override fun getFormat() = Pixmap.Format.Alpha
    override fun useMipMaps() = false
    override fun isManaged() = true

    override fun consumePixmap(): Pixmap {
        TODO("Not yet implemented")
    }

    override fun disposePixmap(): Boolean {
        TODO("Not yet implemented")
    }
}

fun ShaderProgram.safeSetUniform(name: String, valu: Float) {
    if (this.hasUniform(name)) { this.setUniformf(name, valu) }
}
fun ShaderProgram.safeSetUniform(name: String, valu: Int) {
    if (this.hasUniform(name)) { this.setUniformi(name, valu) }
}
//fun com.badlogic.gdx.scenes.scene2d.ui.Slider.changed() = flow {
//    addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
//        override fun changed(event: ChangeEvent?, actor: Actor?) {
//            launch { emit (value) }
//            //block(event, actor)
//        }
//    }) }
//}