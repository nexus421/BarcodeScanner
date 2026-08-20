package bayern.kickner.barcode_scanner_library

import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * ML Kit's [Barcode] type exposes no confidence/score per barcode, so [resolveScanResult] must not invent a
 * "best match" ranking - it forwards ML Kit's own detection list untouched. These tests lock that contract in so
 * a future change can't silently start sorting, filtering or reordering the result.
 */
class ResolveScanResultTest {

    @Test
    fun `empty detection list signals keep scanning`() {
        assertNull(resolveScanResult(emptyList()))
    }

    @Test
    fun `single barcode is forwarded unchanged`() {
        val barcode = mock(Barcode::class.java)

        val result = resolveScanResult(listOf(barcode))

        assertEquals(listOf(barcode), result)
    }

    @Test
    fun `multiple barcodes are forwarded in ML Kit's own order, not resorted`() {
        val first = mock(Barcode::class.java)
        val second = mock(Barcode::class.java)
        val third = mock(Barcode::class.java)
        val detected = listOf(first, second, third)

        val result = resolveScanResult(detected)

        assertEquals(detected, result)
        assertSame(detected, result)
    }
}
