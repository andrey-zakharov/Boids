package me.zakharov.ants.gdx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.viewport.FitViewport
import me.zakharov.Matrix2d
import me.zakharov.ants.model.AntState
import me.zakharov.ants.model.Ants
import me.zakharov.ants.model.plus
import me.zakharov.ants.model.rotatedDeg


class AntsDrawer(private val model: Ants, private val font: BitmapFont = BitmapFont()) : Actor() {
    var enabled = true // show or not
    private val tex = Texture("tex/carpenter-ant-small.png")
    private val texfull = Texture("tex/carpenter-ant-small-full.png")
    private val scaleMat by lazy { Matrix3().setToScaling(
        stage.width / model.width.toFloat(),
        stage.height / model.height.toFloat())
    }

    override fun act(delta: Float) {
        super.act(delta)
        model.act(delta)
    }
    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
        if ( !enabled ) return;
        batch?.let {

            model.forEach { pos, vel, st ->
                val t = if (st == AntState.empty) tex else texfull

                // pos2world
                it.draw(
                    t,
                    pos.x - t.width / 2f,
                    pos.y - t.height / 2f,
                    t.width / 2f, t.height / 2f,
                    t.width.toFloat(), t.height.toFloat(),
                    1f, 1f, vel.angleDeg() - 90,
                    0, 0,
                    t.width, t.height,
                    false, false
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
                val p = pos.mul(scaleMat)
                rectLine(p, p + viewVel, 10f)

                color = Color.GOLD
                val lBound = p + viewVel.rotatedDeg(-model.conf.angleDegs)
                val rBound = p + viewVel.rotatedDeg(model.conf.angleDegs)
                triangle(p.x, p.y, lBound.x, lBound.y, rBound.x, rBound.y)

            }
            // load state
            color = old
        }
    }
}