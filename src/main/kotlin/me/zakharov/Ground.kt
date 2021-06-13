package me.zakharov.me.zakharov

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.KernelAccess
import me.apemanzilla.ktcl.cl10.enqueueWriteBuffer
import me.zakharov.createByteMatrix2d
import me.zakharov.share

//@ExportTo(opencl)
// enum CellType { empty = 0, nest = 1, food = 2, obstacle = 3 } ;
enum class CellType(val code: Byte, val color: Color) {
    Empty(0x0, Color.CLEAR),
    Nest(0x1, Color.BLUE),
    Food(0x2, Color.GREEN),
    Obstacle( 0x3, Color.BROWN)
}

class Ground(
        private val groundTexture: Texture,
        private val ctx: CLContext,
        private val cmd: CLCommandQueue,
        val w: Int, val h: Int
) : Actor() {


    private val m = createByteMatrix2d(w, h)
    internal val shared = ctx.share(m.wrapped, KernelAccess.ReadOnly)
    private val tex: Texture

    init {

        groundTexture.textureData.prepare()
        val px = groundTexture.textureData.consumePixmap()
        val cpx = Pixmap(w, h, Pixmap.Format.RGBA8888).apply {
            filter = Pixmap.Filter.NearestNeighbour
            blending = Pixmap.Blending.None
            drawPixmap(px, 0, 0, px.width, px.height, 0, 0, width, height)
        }

        var c = Color()
        val stats = mutableMapOf<CellType, Int>()

        for (y in 0 until h) {
            for ( x in 0 until w) {
                c.set(cpx.getPixel(x, y))
                val cellType = when {
                    (c.g > c.r && c.g > c.b) -> CellType.Food
                    (c.r == c.g && c.r == c.b && c.r > 0) -> CellType.Nest
                    (c.r > c.g && c.r > c.b) -> CellType.Obstacle
                    else -> CellType.Empty
                }
                m[x, y] = cellType.code
                cpx.drawPixel(x, y, Color.rgba8888(cellType.color))
                stats[cellType] = stats.getOrDefault(cellType, 0) + 1
                //print(buff[x, y])
            }
            //println(y)
        }
        println(stats)
        groundTexture.textureData.disposePixmap()
        tex = Texture(cpx)
        shared.upload(cmd)
    }
//
//    override  fun act(delta: Float) {
//        super.act(delta)
//
//    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
        batch?.let {
            it.draw(tex, 0f, 0f, stage.width, stage.height)
        }
    }
}