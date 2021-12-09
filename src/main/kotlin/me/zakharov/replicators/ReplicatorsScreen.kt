package me.zakharov.me.zakharov.replicators

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
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
import me.zakharov.me.zakharov.replicators.gdx.BacteriaDrawer
import me.zakharov.me.zakharov.replicators.gdx.WorldDrawer
import me.zakharov.me.zakharov.replicators.model.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

class ReplicatorsScreen(private val batch: Batch?,
                        private val skin: Skin,
                        private val input: InputMultiplexer? = Gdx.input.inputProcessor as? InputMultiplexer
) : KtxScreen {

    private val model by lazy {
        BacteriaSystem(
            BacteriaConf(maxAge = 1000), WorldSystem(WorldConf(width = 100, height = 100))
        )
    }
    private var selected: Vector2 = Vector2(Vector2.Zero)
        set(v) {
            if (field != v ) {
                field = v
                bacteriaDrawer.uniformSelectedX = v.x.toInt()
                bacteriaDrawer.uniformSelectedY = v.y.toInt()
            }
        }

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
                        val out = ByteArrayOutputStream()
                        with(PrintStream(out)) {
                            println("selected $selected")
                            val x = selected.x.toInt()
                            val y = selected.y.toInt()
                            val l = model.world.light[x, y]
                            val ms = model.world.moisture[x, y]
                            val mn = model.world.minerals[x, y]
                            val c = GroundType.values()[model.world.cells[x, y].toInt()]
                            println("cell light: %.3f".format(l))
                            println("cell type: $c")
                            println("cell moisture: $ms")
                            println("cell minerals: $mn")

                            val bidx = model.field[x, y]
                            if ( bidx >=0 ) {
                                val b = model[bidx]
                                println("#%d".format(bidx))
                                println("pos: %d x %d".format(b.pos.x.roundToInt(), b.pos.y.roundToInt()))
                                println("age: %.0f%%".format(b.age * 100))
                                println("energy: %.0f%%".format(b.energy * 100))
                                println("current command: %d".format(b.current_command))
                            }
                        }
                        setText(out.toString())
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

            addListener {
                if (it !is InputEvent || it.type != InputEvent.Type.keyTyped) return@addListener false
                when(it.keyCode) {
                    Input.Keys.P -> { model.printDebug(); true }
                    Input.Keys.LEFT -> { selected.x = (selected.x - 1).coerceIn(0f, model.world.cf.width.toFloat()); true }
                    Input.Keys.RIGHT -> { selected.x = (selected.x + 1).coerceIn(0f, model.world.cf.width.toFloat()); true }
                    Input.Keys.UP -> { selected.y = (selected.y - 1).coerceIn(0f, model.world.cf.height.toFloat()); true }
                    Input.Keys.DOWN -> { selected.y = (selected.y + 1).coerceIn(0f, model.world.cf.height.toFloat()); true }
                    else -> false
                }
            }

        }
    }

    private val bacteriaDrawer by lazy { BacteriaDrawer(model) }

    private val scene by lazy {
        Stage(FitViewport(model.world.cf.width.toFloat(), model.world.cf.height.toFloat()), batch).apply {
            addActor(WorldDrawer(model.world))
            addActor(bacteriaDrawer)
            addListener( object: InputListener() {
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
                    val cx = x.coerceIn(0f, model.world.cf.width - 1f)
                    val cy = (model.world.cf.height - y).coerceIn( 0f, model.world.cf.height - 1f)
                    selected = Vector2(floor(cx), floor(cy))
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
                    age = 0f,
                    energy = random.nextFloat()
                )
            )
        }
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
    }

    override fun hide() {
        super.hide()
        input?.apply {
            removeProcessor(scene)
            removeProcessor(ui)
        }
    }
}