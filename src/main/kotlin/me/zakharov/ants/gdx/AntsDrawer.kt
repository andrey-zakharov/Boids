package me.zakharov.ants.gdx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.scenes.scene2d.Actor
import me.zakharov.ants.model.AntState
import me.zakharov.ants.model.Ants


class AntsDrawer(private val model: Ants, private val font: BitmapFont = BitmapFont(true)) : Actor() {
    var enabled = true // show or not
    var showEmpty = false

    private val tex = Texture("tex/carpenter-ant-small.png")
    private val texfull = Texture("tex/carpenter-ant-small-full.png")
    private val texScaleX by lazy { model.width.toFloat() / Gdx.app.graphics.width }
    private val texScaleY by lazy { model.height.toFloat() / Gdx.app.graphics.height }

    private val scaleMat by lazy {
        Matrix3().setToScaling( texScaleX, texScaleY)
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
                // filter
                if ( !showEmpty && st == AntState.empty ) return@forEach
                val t = if ( showEmpty ) if (st == AntState.empty) tex else texfull else tex

                // pos2world
                it.draw(
                    t,
                    pos.x - t.width / 2f,
                    pos.y - t.height / 2f,
//                    stage.width / 2, stage.height/2,
                    t.width / 2f, t.height / 2f,
                    t.width.toFloat(), t.height.toFloat(),
                    texScaleX, texScaleY, vel.angleDeg() - 90,
                    0, 0,
                    t.width, t.height,
                    false, false
                )

                //font.draw(batch, "%.2fx%.2f".format(p[2*i], p[2*i+1]), pos.x, pos.y + 20f )
                // if this ant debug
                // selected
//                if ( debug ) {
//                    font.draw(batch, "%.2fx%.2f".format(vel.x, vel.y), pos.x, pos.y + 20f)
//                    //conf.font.draw(batch, actlast.toString(), pos.x - 10f, pos.y - 20f)
//                    font.draw(batch, st.toString(), pos.x + 10f, pos.y - 20f)
//                }
            }

        }
    }

    override fun drawDebug(shapes: ShapeRenderer?) {
        super.drawDebug(shapes)
        shapes?.apply {
            // save state
            val old = color
            model.forEach { pos, vel, st ->
                //color = Color.RED
                //rectLine(p, p + viewVel, 2f)

                color = if (st == AntState.empty ) Color.GOLD else Color.GREEN
                if (vel.len() > 0 ) {
                    arc(
                        pos//.mul(scaleMat)
                            .x,
                        pos//.mul(scaleMat)
                            .y,
                        vel.len(),
                        /*start = */
                        kotlin.math.atan2(vel.y, vel.x) * MathUtils.radiansToDegrees - model.conf.angleDegs / 2,
                        /*degrees = */
                        model.conf.angleDegs,
                        5
                    )
                }
            }
            color = Color.BROWN
            shapes.rect(-5f, -5f, 10f, 10f)
            // load state
            color = old
        }
    }
}