import me.zakharov.utils.modE
import org.junit.Test
import kotlin.test.assertEquals

class UtilTests {
    @Test fun `modE divides correct`() {
        assertEquals(0, (-4).modE(2))
        assertEquals(1, (-3).modE(2))
        assertEquals(0, (-2).modE(2))
        assertEquals(0, (10).modE(5))
        assertEquals(1, (11).modE(5))
        assertEquals(2, (12).modE(5))
        assertEquals(3, (13).modE(5))
        assertEquals(4, (14).modE(5))
        assertEquals(0, (15).modE(5))
    }
}