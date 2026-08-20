package bayern.kickner.barcode_scanner_library

import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.assertArrayEquals
import org.junit.Test

/**
 * [BarcodeScannerOptions.Builder.setBarcodeFormats] requires at least one format constant and would throw an
 * exception if called with an empty vararg. [resolveBarcodeFormats] is what protects [BarcodeScannerDialogV3]'s
 * constructor from crashing when a caller passes `barcodeFormats = emptyList()`.
 */
class ResolveBarcodeFormatsTest {

    @Test
    fun `empty list falls back to FORMAT_ALL_FORMATS`() {
        val result = resolveBarcodeFormats(emptyList())

        assertArrayEquals(intArrayOf(Barcode.FORMAT_ALL_FORMATS), result)
    }

    @Test
    fun `single format is kept as is`() {
        val result = resolveBarcodeFormats(listOf(Barcode.FORMAT_QR_CODE))

        assertArrayEquals(intArrayOf(Barcode.FORMAT_QR_CODE), result)
    }

    @Test
    fun `duplicate formats are deduplicated`() {
        val result = resolveBarcodeFormats(listOf(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_QR_CODE, Barcode.FORMAT_EAN_13))

        assertArrayEquals(intArrayOf(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_EAN_13), result)
    }

    @Test
    fun `multiple distinct formats are preserved in the given order`() {
        val input = listOf(Barcode.FORMAT_CODE_128, Barcode.FORMAT_QR_CODE, Barcode.FORMAT_EAN_8)

        val result = resolveBarcodeFormats(input)

        assertArrayEquals(input.toIntArray(), result)
    }
}
