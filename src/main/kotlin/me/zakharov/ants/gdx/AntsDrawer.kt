package me.zakharov.ants.gdx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import me.zakharov.ants.model.Ants
import me.zakharov.ants.model.plus
import me.zakharov.ants.model.rotatedDeg


class AntsDrawer(private val model: Ants, private val font: BitmapFont = BitmapFont()) : Actor() {
    private val tex = Texture("tex/carpenter-ant-small.png")

    override fun act(delta: Float) {
        super.act(delta)
        model.act(delta)
    }
    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
        batch?.let {
            val scaleStageX = stage.width / model.width.toFloat()
            val scaleStageY = stage.height / model.height.toFloat()
            model.forEach { pos, vel, st ->

                // pos2world
                it.draw(
                    tex,
                    pos.x * scaleStageX - tex.width / 2f,
                    stage.height - pos.y * scaleStageY + tex.height / 2f,
                    tex.width / 2f, tex.height / 2f,
                    tex.width.toFloat(), tex.height.toFloat(),
                    1f, 1f, 90 - vel.angleDeg(),
                    0, 0,
                    tex.width, tex.height,
                    false, true
                )

                //font.draw(batch, "%.2fx%.2f".format(p[2*i], p[2*i+1]), pos.x, pos.y + 20f )
                // if this ant debug
                // selected
                if ( debug ) {
                    font.draw(batch, "%.2fx%.2f".format(vel.x, vel.y), pos.x, pos.y + 20f)
                    //conf.font.draw(batch, actlast.toString(), pos.x - 10f, pos.y - 20f)
                    font.draw(batch, st.toString(), pos.x + 10f, pos.y - 20f)
                }
            }
        }
    }

    override fun drawDebug(shapes: ShapeRenderer?) {
        super.drawDebug(shapes)
        shapes?.apply {
            // save state
            val old = color
            model.forEach { pos, vel, st ->
                color = Color.RED
                val viewVel = vel
                rectLine(pos, pos + viewVel, 10f)

                color = Color.GOLD
                val lBound = pos + viewVel.rotatedDeg(-model.conf.angleDegs)
                val rBound = pos + viewVel.rotatedDeg(model.conf.angleDegs)
                triangle(pos.x, pos.y, lBound.x, lBound.y, rBound.x, rBound.y)

            }
            // load state
            color = old
        }
    }
}