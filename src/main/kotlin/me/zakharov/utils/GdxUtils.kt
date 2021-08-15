package me.zakharov.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.glutils.FloatTextureData
import java.nio.ByteBuffer


internal class WrappedFloatTextureData(w: Int, h: Int, private val buff: ByteBuffer)
    : FloatTextureData(w, h, GL30.GL_R32F, GL30.GL_RED, 0, false) {
    override fun consumeCustomData(target: Int) {
        Gdx.gl.glTexImage2D(target, 0, GL30.GL_R32F, width, height, 0, GL30.GL_RED, GL20.GL_FLOAT, buff)
    }
}