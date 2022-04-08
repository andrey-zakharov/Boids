package me.zakharov.replicators

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
import me.zakharov.replicators.model.*
import me.zakharov.utils.onClick
import me.zakharov.utils.print
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

interface IPlatformService {
    val memUsed: Long
}

data class Coords(val x: Int, val y: Int) {
    override fun toString() = "%d x %d".format(x, y)
    companion object {
        val ZERO = Coords(0, 0)
        private val NB = listOf(Coords(-1, 0), Coords(1, 0), Coords(0, -1), Coords(0, 1))
        // iterator for NBs TBD

    }

    fun coerseIn(min: Coords, max: Coords) =
        // leads to obsecive GCs ;) lets try
        Coords(x.coerceIn(min.x, max.x), y.coerceIn(min.y, max.y))

    // for squares impl
    // checks for bounds are on callee
    fun getDir(dir: Int) = Coords(x + NB[dir].x, y + NB[dir].y)
    fun getLeft() = getDir(0)
    fun getRight() = getDir(1)
    fun getUp() = getDir(2)
    fun getDown() = getDir(3)
    fun allNeibs(block: () -> Unit) {

    }

}

// class BoundedCoords?

interface IPlatformServiceAcceptor {
    fun accept(service: IPlatformService)
}
class ReplicatorsScreen(private val batch: Batch?,
                        private val skin: Skin,
                        private val input: InputMultiplexer? = Gdx.input.inputProcessor as? InputMultiplexer
) : KtxScreen, IPlatformServiceAcceptor {

    companion object {
        private val model by lazy {
            BacteriaSystem(
                BacteriaConf(maxAge = 1000), WorldSystem(WorldConf(width = 75, height = 75))
            )
        }
        internal val BoundingBox by lazy { Coords(model.world.cf.width, model.world.cf.height) }
    }

    private var selected: Coords = Coords.ZERO
        get() = Coords(bacteriaDrawer.uniformSelectedX, bacteriaDrawer.uniformSelectedY)
        set(v) {
            if (field != v ) {
                field = v
                bacteriaDrawer.uniformSelectedX = v.x
                bacteriaDrawer.uniformSelectedY = v.y
            }
        }

    private var track: Int = 0

    //private var selectedObj: Bacteria? = null

    // runtime state
    private var pause: Boolean = false
    private var step: Boolean = false
    private var memused = 0L
    private val console by lazy {
        object: Label("console", skin) {
            override fun act(delta: Float) {
                super.act(delta)
                //setText(bacteriaDrawer.showLayer.value.toString(2))
            }
        }
    }

    override fun accept(service: IPlatformService) {
        //bind memused = service.memUsed

    }

    private var drawBacterias: Boolean = true
        set(v) {
            if ( field == v ) return;
            field = v;
            bacteriaDrawer.isVisible = v
        }
    private var drawGround: Boolean = true
        set(v) {
            if ( field == v ) return;
            field = v;
            groundDrawer.isVisible = v
        }

    private val ui by lazy {
        val random = Random(System.currentTimeMillis())
        Stage(ScreenViewport(/*Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()*/), batch).apply {
            //isDebugAll = true
            // console .log here
            addActor(VerticalGroup().apply {
                top()
                setFillParent(true)
                addActor(object : Label("fps", skin) {
                    override fun act(delta: Float) {
                        super.act(delta)
                        setText("""
                            fps: ${Gdx.graphics.framesPerSecond}
                            frameId: ${Gdx.graphics.frameId}
                            count: ${model.current_idx}
                            mem:${memused} bytes""".trimIndent())
                    }
                })
                addActor(console)
            })

            addActor(VerticalGroup().apply {
                bottom()
                right()
                setFillParent(true)
                space(3f)

                // info cell and bacteria
                addActor(object:Label("selected", skin) {
                    override fun act(delta: Float) { // tbd only when dirty
                        super.act(delta)
                        val out = ByteArrayOutputStream()
                        with(PrintStream(out)) {
                            println("season: %.3f".format(model.world.seasonedUltraviolet))
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
                            if ( bidx < 0 ) return@with
                            model[bidx].run {
                                println("#%d".format(model.field[x, y]))
                                accept(this@with)

                                /*for( i in 0 until model.cf.genLen ) {
                                    print("${b.gen[i]} ")
                                    if ( i > 0 && b.gen[i-1] == 0.toByte() && b.gen[i] == 0.toByte() ) {
                                        break;
                                    }
                                    if ( (i+1) % 10 == 0 ) println()
                                }*/
                                //dispose()
                            }
                        }
                        setText(out.toString())
                    }
                })

                /// gen program list view
                addActor(object: ScrollPane(
                    object: com.badlogic.gdx.scenes.scene2d.ui.List<String>(skin) {
                        override fun act(delta: Float) {
                            super.act(delta)
                            val x = this@ReplicatorsScreen.selected.x
                            val y = this@ReplicatorsScreen.selected.y
                            val bidx = model.field[x, y]
                            if ( bidx < 0 ) return
                            model[bidx].also { b->
                                val items = b.genStr.values.toTypedArray()
                                this.setItems(*items)
                                try {
                                    val prefix = b.current_command.toString(10)
                                    this.selectedIndex = items.indexOfFirst { it.startsWith(prefix) }

                                } catch (e: Throwable) {
                                    println(e)
                                    println(b.genStr.values.joinToString(", "))
                                    println(b.gen.print())
                                    println(b.current_command)
                                }
                            }
                        }

                }, skin) {

                })

                bacteriaDrawer.controls.forEach { addActor(it) }


                addActor(CheckBox("draw bacterias", skin).also {
                    //
                    drawBacterias
                    it.isChecked = drawBacterias
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            drawBacterias = it.isChecked
                        }
                    })
                })

                addActor(CheckBox("draw ground", skin).also {
                    it.isChecked = drawGround
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            drawGround = it.isChecked
                        }
                    })
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

                addActor(TextButton("test", skin).apply {
                    onClick { t, e, v ->


                    }

                })
            })

            addListener {
                if (it !is InputEvent || it.type != InputEvent.Type.keyUp) return@addListener false
                when(it.keyCode) {
                    Input.Keys.P -> { model.printDebug(); true }
                    Input.Keys.LEFT -> { selected = selected.getLeft().coerseIn(Coords.ZERO, BoundingBox); true }
                    Input.Keys.RIGHT -> { selected = selected.getRight().coerseIn(Coords.ZERO, BoundingBox); true }
                    Input.Keys.UP -> { selected = selected.getUp().coerseIn(Coords.ZERO, BoundingBox); true }
                    Input.Keys.DOWN -> { selected = selected.getDown().coerseIn(Coords.ZERO, BoundingBox); true }
                    Input.Keys.PAGE_UP -> { track = (track + 1) % model.current_idx; true }
                    Input.Keys.PAGE_DOWN -> { track = (track - 1) % model.current_idx; true }
                    Input.Keys.SPACE -> { pause = !pause; true }
                    Input.Keys.ENTER -> { pause = true; step = true; true }
                    else -> false
                }
            }

        }
    }

    private val groundDrawer by lazy { WorldDrawer(model.world) }
    private val bacteriaDrawer by lazy { BacteriaDrawer(model, skin) }

    private val scene by lazy {
        Stage(FitViewport(model.world.cf.width.toFloat(), model.world.cf.height.toFloat()), batch).apply {
            addActor(groundDrawer)
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
                // camera 2 world stuff
                override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                    super.touchDragged(event, x, y, pointer)
                    val cf = model.world.cf

                    model.setField(
                        x.roundToInt().coerceIn(0, cf.width-1),
                        (model.world.cf.height - y).roundToInt().coerceIn(0, cf.height-1), GroundType.obstacle)
                }
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    // screen2world
                    val cx = x.coerceIn(0f, model.world.cf.width - 1f)
                    val cy = (model.world.cf.height - y).coerceIn( 0f, model.world.cf.height - 1f)
                    selected = Coords(floor(cx).toInt(), floor(cy).toInt())
                }
            })
        }
    }

    fun addRandom(count: Int = 100) {
        val random = Random(System.currentTimeMillis())
        model.freeCells.shuffled(random).take(
            count.coerceAtMost(model.world.cf.totalCells)
        ).forEach {
            model.add(
                Bacteria(
                    pos = Vector2(it.first.toFloat(), it.second.toFloat()),
                    age = random.nextFloat() * 0.1f,
                    energy = random.nextFloat()
                )
            )
        }
    }

    override fun render(delta: Float) {
        if ( !pause || step ) {
            model.act(delta)
            step = false
        }

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