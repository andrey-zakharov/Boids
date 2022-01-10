package me.zakharov.me.zakharov.replicators.gdx

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import me.zakharov.utils.BufferTextureData

internal class BufferDrawer(private val texData: BufferTextureData) : Sprite (
    Texture(texData).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    })
{
     override fun draw(batch: Batch?, alphaModulation: Float) {
         texture.load(texData)
         super.draw(batch, alphaModulation)
     }
}