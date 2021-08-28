package me.zakharov.ants.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import me.apemanzilla.ktcl.CLCommandQueue
import me.apemanzilla.ktcl.CLContext
import me.apemanzilla.ktcl.cl10.KernelAccess
import me.apemanzilla.ktcl.cl10.enqueueWriteBuffer
import me.zakharov.Matrix2d
import me.zakharov.createByteMatrix2d
import me.zakharov.share
import me.zakharov.utils.IHeadlessActor
import org.lwjgl.system.CallbackI.D




//@ExportTo(opencl)
// enum CellType { empty = 0, nest = 1, food = 2, obstacle = 3 } ;
enum class GroundType(val code: Byte, val color: Color) {
//    Empty(0x0, Color.valueOf("FFC500ff")),
    Empty(0x0, Color.valueOf("FFC50015")),
    Nest(0x1, Color.BLUE),
    Food(0x2, Color.GREEN),
    Obstacle( 0x3, Color.BROWN)
}

class CellTypeCollector {
    val sum = Vector2.Zero
    //var bb = Rectangle(Int.MAX_VALUE, Int.MAX_VALUE, 0, 0)
    var bbx = Int.MAX_VALUE
    var bby = Int.MAX_VALUE
    var bbw = 0
    var bbh = 0
    private val stats = mutableMapOf<GroundType, Int>()
    operator fun invoke(x: Int, y: Int, v: GroundType) {
        if ( v == GroundType.Nest ) {
            sum.add(Vector2(x.toFloat(), y.toFloat()))
            if ( bbx > x ) bbx = x
            if ( bby > y ) bby = y
            if ( bbx + bbw < x ) bbw = x - bbx
            if ( bby + bbh < x ) bbh = y - bby
        }

        stats[v] = stats.getOrDefault(v, 0) + 1
    }
    // final
    fun getMedian() = Vector2(sum.x / stats[GroundType.Nest]!!, sum.y / stats[GroundType.Nest]!!)
    fun getBoundingBox() = Rectangle(bbx.toFloat(), bby.toFloat(), bbw.toFloat(), bbh.toFloat())
    fun reset() {
        sum.set(Vector2.Zero)
        //var bb = Rectangle(Int.MAX_VALUE, Int.MAX_VALUE, 0, 0)
        bbx = Int.MAX_VALUE
        bby = Int.MAX_VALUE
        bbw = 0
        bbh = 0
    }
    override fun toString() = this::class.simpleName + ": " + stats.toString()
}

/* Euclidean division
 check :
 https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/divmodnote-letter.pdf
 https://github.com/typelevel/algebra/pull/172#issuecomment-248813229
 https://youtrack.jetbrains.com/issue/KT-14650
 */
fun Int.modE(d: Int): Int {
    var r = this % d
    if (r < 0) {
        r = if (d > 0) r + d else r - d
    }
    return r
}

class Ground(
        private val ctx: CLContext,
        private val cmd: CLCommandQueue,
        val width: Int, val height: Int,
        paintOnMatrix: Ground.(m: Matrix2d<Byte>) -> Unit = {}
) : IHeadlessActor {

    // wrap_index
    // boundsStrategy
    // @ClCode
    private fun prepare_index(x: Int, y: Int): Int {
        return x.modE(width) + y.modE(height) * width
    }

    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    private val _onRefresh = MutableSharedFlow<Unit>()
    val onRefresh: SharedFlow<Unit> get() = _onRefresh

    private val m = createByteMatrix2d(width, height)
    internal val shared = ctx.share(m.buff, KernelAccess.ReadWrite)
    operator fun set(x: Int, y: Int, v: GroundType) {
        report(x, y, v)
//        m[x, y] = v.code
        m.buff.put(prepare_index(x, y), v.code)
        scope.launch { _onRefresh.emit(Unit) }
    }

    operator fun get(x: Int, y: Int) = m[x, y] // : GroundType = GroundType.m[x, y]

    // stats field

    internal val report = CellTypeCollector()

    override fun act(delta: Float) {
    }

    init {
        paintOnMatrix(m)
        update(true)
    }

    fun update(upload: Boolean = false) {
        scope.launch { _onRefresh.emit(Unit) }
        if ( upload ) {
            cmd.enqueueWriteBuffer(shared.buff, shared.remoteBuff)
        }
    }

    fun getNestCoords(index: Int = 0) = report.getMedian()

    fun debugString():String {
        val sb = StringBuilder()
        for( y in 0 until height ) {
            for( x in 0 until width ) {
                sb.append(when(m[x, y]) {
                    GroundType.Nest.code -> 'N'
                    GroundType.Empty.code -> ' '
                    GroundType.Food.code -> '.'
                    GroundType.Obstacle.code -> '#'
                    else -> throw RuntimeException("unknown ground type")
                })
            }
            sb.append('\n')
        }
        return sb.toString()
    }
}