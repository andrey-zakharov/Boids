package me.zakharov.me.zakharov.replicators

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxScreen
import me.zakharov.me.zakharov.ants.AntsScreen
import me.zakharov.me.zakharov.replicators.gdx.BacteriaDrawer
import me.zakharov.me.zakharov.replicators.gdx.WorldDrawer
import me.zakharov.me.zakharov.replicators.model.*
import kotlin.math.roundToInt
import kotlin.random.Random

class ReplicatorsScreen(private val batch: Batch?,
                        private val skin: Skin,
                        private val input: InputMultiplexer? = Gdx.input.inputProcessor as? InputMultiplexer
) : KtxScreen {

    private val model by lazy {
        BacteriaSystem(
            BacteriaConf(maxAge = 10000), WorldSystem(WorldConf(width = 50, height = 50))
        )
    }
    private var selected: Int = -1

    private val ui by lazy {
        val random = Random(System.currentTimeMillis())
        Stage(ScreenViewport(/*Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()*/), batch).apply {
            //isDebugAll = true
            addActor(HorizontalGroup().apply {
                top()
                rowCenter()
                setFillParent(true)
                addActor(object: Label("total: ", skin ) {
                    override fun act(delta: Float) {
                        super.act(delta)
                        setText("total: ${model.current_idx}")
                    }
                })

            })

            addActor(VerticalGroup().apply {
                bottom()
                right()
                setFillParent(true)
                space(3f)
                addActor(object:Label("selected", skin) {
                    override fun act(delta: Float) { // tbd only when dirty
                        super.act(delta)
                        setText("selected $selected")
                        if ( selected >=0 && selected < model.current_idx ) {
                            val b = model[selected]
                            setText("pos: %d x %d\nage: %f\nenergy: %f".format(
                                b.pos.x.roundToInt(), b.pos.y.roundToInt(),
                                b.age, b.energy
                            ))
                        }
                    }
                })

                addActor(object : Label("fps: ", skin) {
                    override fun act(delta: Float) {
                        super.act(delta)
                        setText("fps: ${Gdx.graphics.framesPerSecond}\nframeId: ${Gdx.graphics.frameId}\ncount: ${model.current_idx}")
                    }
                })
                addActor(object : TextButton("add bacteria", skin) {

                }.also {
                    it.addListener( object: ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            val x = random.nextInt(model.world.cf.width).toFloat()
                            val y = random.nextInt(model.world.cf.height).toFloat()
                            model.add(Bacteria(
                                pos = Vector2(x, y),
                                energy = random.nextFloat()
                            ))
                        }
                    })
                })
                addActor(object : TextButton("add 100x bacteria", skin) {

                }.also {
                    it.addListener( object: ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            addRandom()
                        }
                    })
                })
            })
        }
    }

    private val bacteriaDrawer by lazy { BacteriaDrawer(model) }

    private val scene by lazy {
        Stage(FitViewport(model.world.cf.width.toFloat(), model.world.cf.height.toFloat()), batch).apply {
            addActor(WorldDrawer(model.world))
            addActor(bacteriaDrawer)
            addListener( object: InputListener() {
                override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                    super.touchUp(event, x, y, pointer, button)
                    println("t $x, $y")
                }

                override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                    bacteriaDrawer.hovered.set(
                        x / model.world.cf.width,
                        1 - y / model.world.cf.height
                    )

                    return super.mouseMoved(event, x, y)
                }
            })
            addListener( object: ClickListener() {
                override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                    super.touchDragged(event, x, y, pointer)
                    val cf = model.world.cf

                    model.setField(
                        x.roundToInt().coerceIn(0, cf.width-1),
                        (model.world.cf.height - y).roundToInt().coerceIn(0, cf.height-1), GroundType.obstacle)
                }
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    assert( x >= 0 && x <= model.world.cf.width)
                    assert( y >= 0 && y <= model.world.cf.height)
                    val f = model.field[x.roundToInt(), (model.world.cf.height - y).roundToInt()]
                    if ( f > 0 ) {
                        val index = f - 1
                        onSelect(index)
                    }
                }
            })
        }
    }

    fun addRandom(count: Int = 100) {
        val random = Random(System.currentTimeMillis())
        for( i in 0..count ) {
            val x = random.nextInt(model.world.cf.width).toFloat()
            val y = random.nextInt(model.world.cf.height).toFloat()
            model.add(
                Bacteria(
                    pos = Vector2(x, y),
                    age = random.nextFloat(),
                    energy = random.nextFloat()
                )
            )
        }
    }
    
    fun onSelect(idx: Int) {
        bacteriaDrawer.selected = idx
        selected = idx
    }

    override fun render(delta: Float) {
        model.act(delta)
        arrayOf(scene, ui).forEach { s ->
            s.act(delta)
            s.viewport.apply(s == ui)
            s.batch.projectionMatrix = s.camera.combined
            s.draw()
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        scene.viewport.update(width, height)
        ui.viewport.update(width, height)
    }

    override fun show() {
        super.show()
        input?.apply {
            addProcessor(scene)
            addProcessor(ui)
        }
        println("Max bacts: ${model.max}")
        addRandom(10)
        selected = 0
    }

    override fun hide() {
        super.hide()
        input?.apply {
            removeProcessor(scene)
            removeProcessor(ui)
        }
    }
}