package me.zakharov.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.TextureData
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.GdxRuntimeException
import java.nio.ByteBuffer
import java.nio.IntBuffer

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

internal class FloatTextureData(w: Int, h: Int, buff: ByteBuffer)
    : BufferTextureData(w, h, BufferWrapperType.FloatTexture, buff)

internal class IntTextureData(w: Int, h: Int, buff: ByteBuffer)
    : BufferTextureData(w, h, BufferWrapperType.IntTexture, buff)

internal class ByteTextureData(w: Int, h: Int, buff: ByteBuffer)
    : BufferTextureData(w, h, BufferWrapperType.ByteTexture, buff)



enum class BufferWrapperType(
    /**
     * Specifies the number of color components in the texture.
     * Must be one of base internal formats, one of the sized internal formats, or one of the compressed
     * internal formats. See https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glTexImage2D.xhtml
     */
    val internalFormat: Int,
    /**
     * Specifies the format of the pixel data.
     * The following symbolic values are accepted: GL_RED, GL_RG, GL_RGB, GL_BGR, GL_RGBA, GL_BGRA,
     * GL_RED_INTEGER, GL_RG_INTEGER, GL_RGB_INTEGER, GL_BGR_INTEGER, GL_RGBA_INTEGER, GL_BGRA_INTEGER,
     * GL_STENCIL_INDEX, GL_DEPTH_COMPONENT, GL_DEPTH_STENCIL.
     */
    val format: Int,
    /**
     * Specifies the data type of the pixel data.
     * The following symbolic values are accepted: GL_UNSIGNED_BYTE, GL_BYTE, GL_UNSIGNED_SHORT,
     * GL_SHORT, GL_UNSIGNED_INT, GL_INT, GL_HALF_FLOAT, GL_FLOAT, GL_UNSIGNED_BYTE_3_3_2,
     * GL_UNSIGNED_BYTE_2_3_3_REV, GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV,
     * GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, GL_UNSIGNED_SHORT_5_5_5_1,
     * GL_UNSIGNED_SHORT_1_5_5_5_REV, GL_UNSIGNED_INT_8_8_8_8, GL_UNSIGNED_INT_8_8_8_8_REV,
     * GL_UNSIGNED_INT_10_10_10_2, and GL_UNSIGNED_INT_2_10_10_10_REV.
     */
    val type: Int) {
    ByteTexture(GL30.GL_R8I, GL30.GL_RED_INTEGER, GL20.GL_BYTE),
    FloatTexture(GL30.GL_R32F, GL30.GL_RED, GL20.GL_FLOAT),
    IntTexture(GL30.GL_R32I, GL30.GL_RED_INTEGER, GL20.GL_INT),
}

internal open class BufferTextureData(
    val w: Int, val h: Int, val c: BufferWrapperType, private val buff: ByteBuffer
) : TextureData {
    private var isPrepared = false

    override fun getType() = TextureData.TextureDataType.Custom
    override fun isPrepared() = isPrepared

    override fun prepare() {
        if (isPrepared) throw GdxRuntimeException("Already prepared")
        isPrepared = true
    }

    override fun consumeCustomData(target: Int) {
        assert( target == GL20.GL_TEXTURE_2D)
        //buff.print()
        Gdx.gl.glTexImage2D(target, 0, c.internalFormat, width, height, 0, c.format, c.type, buff)
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

fun ShaderProgram.safeSetUniform1iv(name: String, b: IntBuffer, size: Int = b.capacity(), offset: Int = 0) {
    if (hasUniform(name)) {
        //checkManaged()
        val location = fetchUniformLocation(name, false)
        if ( location >= 0 ) {
            Gdx.gl20.glUniform1iv(location, size, b)
        }
    }
}

fun ShaderProgram.safeSetUniform(name: String, v1: Int, v2: Int) {
    if (hasUniform(name)) { setUniformi(name, v1, v2) }
}
fun ShaderProgram.safeSetUniform(name: String, v1: Float, v2: Float) {
    if (hasUniform(name)) { setUniformf(name, v1, v2) }
}
fun ShaderProgram.safeSetUniform(name: String, valu: Float) {
    if (hasUniform(name)) { setUniformf(name, valu) }
}
fun ShaderProgram.safeSetUniform(name: String, valu: Int) {
    if (hasUniform(name)) { setUniformi(name, valu) }
}
//fun com.badlogic.gdx.scenes.scene2d.ui.Slider.changed() = flow {
//    addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
//        override fun changed(event: ChangeEvent?, actor: Actor?) {
//            launch { emit (value) }
//            //block(event, actor)
//        }
//    }) }
//}

fun interface onUpdateListener {
    fun onUpdate(target: Actor?, event: ChangeListener.ChangeEvent?, value: Any?): Unit
}

fun Button.onClick(listener: onUpdateListener) {
    addListener(object: ChangeListener() {
        override fun changed(event: ChangeEvent?, actor: Actor?) {
            listener.onUpdate(actor, event, null)
        }
    } )
}