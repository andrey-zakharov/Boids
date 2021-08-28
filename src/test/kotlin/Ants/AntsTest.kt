/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package Ants

import com.badlogic.gdx.math.Vector2
import me.apemanzilla.ktcl.cl10.createCommandQueue
import me.apemanzilla.ktcl.cl10.createContext
import me.apemanzilla.ktcl.cl10.getDefaultDevice
import me.apemanzilla.ktcl.cl10.getPlatforms
import me.zakharov.ants.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AntsTest {

        // GdxNativesLoader.load();
    val device = getPlatforms()[0].getDefaultDevice()!!
    val ctx = device.createContext()
    val cmd = device.createCommandQueue(ctx)

    private val antsConfig3x3 = AntsConfig(
        width = 3,
        height = 3,
        totalCount = 1,
        maxSpeed = 1.5f,
        angleDegs =  90f,
        //clopts = arrayOf("DEBUG", "DEBUG_PATHFIND", "DEBUG_QUEUE")
    )


    private val antsConfig5x5 = AntsConfig(
        width = 5,
        height = 5,
        totalCount = 1,
        maxSpeed = 1.5f,
        angleDegs =  90f,
        //clopts = arrayOf("DEBUG", /*"DEBUG_PATHFIND", "DEBUG_QUEUE"*/)
    )

    @Test fun `velocity changes on food trail`() {
        val ground = Ground(ctx, cmd, 3, 3) {
            this[0, 0] = GroundType.Nest
        }
        val pheromones = Pheromones(ctx, cmd, 3, 3)
        pheromones.m[2, 0] = PherType.food_trail.v
        val ants = Ants(antsConfig3x3, ctx, cmd, ground, pheromones)

//        ants.ejectAntsFromNest()
        // move forward
        ants.set(0, AntSnapshot(Vector2(1f, 0f), Vector2(1f, 0f), AntState.empty))
        val s = ants.snapshot()
        assertEquals(1, s.size)
        assertEquals(Vector2(1f, 0f), s[0].pos)

        // simple step test
        ants.act(1f)

        assertTrue( pheromones.m[1, 0] != PherType.none.v, "ant should leaves trail")
        val afs = ants.snapshot()
        assertEquals(Vector2(2f, 0f), afs[0].pos)
        assertEquals(Vector2(antsConfig3x3.maxSpeed, 0f), afs[0].vel)// not true


        //val bfs = ants.snapshot()
        //println(bfs[0])
    }

    @Test fun `ant should avoid obstacles`() {
        println(" == obstacleTest == ")
        val ground = Ground(ctx, cmd, antsConfig3x3.width, antsConfig3x3.height) {
            this[0, 0] = GroundType.Nest
            this[1, 0] = GroundType.Obstacle
        }

        val ants = Ants(antsConfig3x3, ctx, cmd, ground)
        ants.set(0, AntSnapshot(Vector2(0f, 0f), Vector2(1f, 0f), AntState.empty))
        ants.act(1f)
        println( ground.debugString() )

        val ant = ants.snapshot()[0]
               println(ant)
        assertFalse { ant.pos == Vector2(1f, 0f) }


    }

    @Test fun `ant should take food`() = with ( Ground(ctx, cmd, antsConfig3x3.width, antsConfig3x3.height) {
        this[0, 0] = GroundType.Nest
        this[1, 1] = GroundType.Food
    }) ground@ {
        with( Ants(antsConfig3x3, ctx, cmd, this@ground)) {
            set(0, AntSnapshot(Vector2(.5f, .5f), Vector2.Zero, AntState.empty))
            act(1f) // to update vel
            println(snapshot()[0])
            act( 1f ) // to get into point
            val ant = snapshot()[0]
            println( this@ground.debugString() )
            println(snapshot()[0])
            assertEquals(AntState.full, ant.state)
            assertEquals(GroundType.Empty.code, this@ground[1, 1])
        }

    }

    @Test fun `ant should find path around obtacle and take food behind obstacle`() = with (
        Ground(ctx, cmd, antsConfig5x5.width, antsConfig5x5.height) ) {
        this[0, 0] = GroundType.Nest
        this[-2, -2] = GroundType.Food
        this[-1, -1] = GroundType.Obstacle
        update(true)
        println(this.debugString())

        with ( Ants(antsConfig5x5, ctx, cmd, this) ) {
            set(0, AntSnapshot(Vector2.Zero, Vector2.Zero, AntState.empty))
            act(1f)
            act(1f)
            act(1f)
            act(1f)
            act(1f)
            act(1f)
            assertEquals(AntState.full, snapshot()[0].state)
            println(snapshot().joinToString(", "))

        }
    }
}

