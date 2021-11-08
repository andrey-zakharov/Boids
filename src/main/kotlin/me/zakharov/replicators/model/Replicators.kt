package me.zakharov.me.zakharov.replicators.model

import com.badlogic.gdx.math.Vector2
import me.zakharov.Const.FLOAT_SIZE
import java.nio.ByteBuffer

const val GEN_LENGTH: Int = 80 // 1 byte is enough for size
const val MAX_ENERGY = 100f // just dummy
const val MEM_BITS_LEN = 3 // 3 bits mem
/// Moore neighborhood
fun ByteBuffer.getDir() = get() % 9


enum class GroundType {
    empty,
    obstacle,
    enemy,
    friend,
    food
}

enum class COMMAND(final: Boolean = false, block: ByteBuffer.(state: Bacteria) -> Unit) {
    jmp(false, { state ->
        state.current_command = (get() % GEN_LENGTH).toByte()
    }),

    /**
     *
     */
    rotate_rel(false, {state -> }),

    rotate_abs(false, {state -> }),
    move_rel(false, {state -> }),
    eat_sun(true, {state -> }),
    eat_own_minerals(true, {state -> }),
    harvest_minerals(true, {state -> }),
    count_around(false, {state ->
        getDir()
    }), // with register. returns count
    look_around( false, { state ->
        val dir = getDir()
    }),
    eat(true, { state ->
        val dir = getDir()
        // return eaten or not
    }),

    compare_energy( false, { s -> s.energy < get() }),
    compare_sun( false, { s -> s.cell.light < get() }),
    compare_minerals( false, { s -> s.cell.minerals < get() }),
    compare_age( false, { s -> s.age < get() } ),
    save_value( false, { s ->
        val memidx = get() % MEM_BITS_LEN
        val memval = get() % 2
        s.memSave(memidx.toByte(), memval.toByte())
    } ),
    load_value( false, { s ->
        val memidx = get() % MEM_BITS_LEN
        s.memLoad(memidx.toByte())
    }),
    save_global_value( false, { s -> }),
}


data class Cell(
    var light: Float = 0f,
    var minerals: Float = 0f,
    var moisture: Float = 0f,
) {
    companion object {
        val bytesize = FLOAT_SIZE * 3
    }
}


