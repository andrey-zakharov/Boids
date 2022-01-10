package me.zakharov.replicators.model

import com.badlogic.gdx.math.Vector2
import java.io.PrintStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.roundToInt

data class Bacteria(
    val pos: Vector2,
    val gen: ByteArray = ByteArray(GEN_LENGTH) { 0 },
    var current_command: Byte = 0, // for 80 GEN_LENGTH
    var age: Float = 0f,
    var energy: Float = 1f,
    var cell: Cell = Cell(),
) {
    fun accept(out: PrintStream) = with(out) {
        println("pos: %d x %d".format(pos.x.roundToInt(), pos.y.roundToInt()))
        println("age: %.0f%%".format(age * 100))
        println("energy: %.0f%%".format(energy * 100))
        println("current command: %d".format(current_command))
    }

    val genStr by lazy {

        // current command is byte number
        // we need current index in command list
        // eof = \0 \0? no, reaching 0 cc again.
        //val genr = gen.slice(0 until GEN_LENGTH)
        //while(genr.isNotEmpty()) {
        //}
        val bb = ByteBuffer.wrap(gen)
        val res = TreeMap<Int, String>()

        while(bb.hasRemaining() && res.size < gen.size) {
            val idx = bb.position()

            if ( res.containsKey(idx) ) { // cycle detected
                res[res.size] = "CYCLE DETECTED"
                break
            }

            val cmd = commands.values()[bb.get() % commands.values().size]

            //kotlin.io.println("found $cmd for #c = $c, my pos = ${pos.x} x ${pos.y}")
            //gen.sliceArray(0 until GEN_LENGTH).takeWhile { b ->
            //    val cmdtype = commands.values()[b % commands.values().size]
            cmd.parseArgs(cmd, bb)

            when(cmd) {
                commands.Jmp -> {
                    val next_command = cmd.args.first()
                    res[idx] = "$idx jmp to $next_command"
                    if ( next_command == 0x0.toByte()) break
                    bb.position(next_command.toInt())

                }
                commands.MoveRandom -> {
                    res[idx] = "$idx move random"
                }
                commands.Move -> {
                    val dir = Dirs.values()[cmd.args.first().toInt() /*% Dirs.values().size*/]
                    res[idx] = "$idx move $dir"
                }
                commands.EatLight -> {
                    res[idx] = "$idx eat light"
                }
                commands.Harvest -> {
                    res[idx] = "$idx harvest"
                }
                commands.EatBacteria -> {
                    val dir = Dirs.values()[cmd.args.first().toInt() /*% Dirs.values().size*/]
                    res[idx] = "$idx eat from $dir"
                }
                else -> res[idx] = "${bb.position()} UNKNOWN $cmd"

            }
        }

        res
    }

    fun memSave(i: Byte, v: Byte) {

    }

    fun memLoad(i: Byte): Byte {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bacteria

        if (pos != other.pos) return false
        if (!gen.contentEquals(other.gen)) return false
        if (current_command != other.current_command) return false
        if (age != other.age) return false
        if (energy != other.energy) return false
        if (cell != other.cell) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pos.hashCode()
        result = 31 * result + gen.contentHashCode()
        result = 31 * result + current_command
        result = 31 * result + age.hashCode()
        result = 31 * result + energy.hashCode()
        result = 31 * result + cell.hashCode()
        return result
    }
}