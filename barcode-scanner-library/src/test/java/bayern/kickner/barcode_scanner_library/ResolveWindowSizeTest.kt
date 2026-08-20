package bayern.kickner.barcode_scanner_library

import android.view.ViewGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [resolveWindowSize] is what [BarcodeScannerDialogV3] uses to size its dialog window per [DialogSize]. These
 * tests lock in that [DialogSize.Fullscreen] always means "fill the window" (not a computed 100% pixel value,
 * which could differ from true edge-to-edge due to rounding) and that the other sizes actually scale relative
 * to each other and to the given screen dimensions.
 */
class ResolveWindowSizeTest {

    @Test
    fun `fullscreen always resolves to MATCH_PARENT regardless of screen size`() {
        val (width, height) = resolveWindowSize(DialogSize.Fullscreen, 1080, 2400)

        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, width)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, height)
    }

    @Test
    fun `small size scales down from the actual screen dimensions`() {
        val (width, height) = resolveWindowSize(DialogSize.Small, 1000, 2000)

        assertEquals((1000 * DialogSize.Small.widthFraction).toInt(), width)
        assertEquals((2000 * DialogSize.Small.heightFraction).toInt(), height)
    }

    @Test
    fun `large is bigger than medium is bigger than small for the same screen`() {
        val screenWidth = 1080
        val screenHeight = 2400

        val small = resolveWindowSize(DialogSize.Small, screenWidth, screenHeight)
        val medium = resolveWindowSize(DialogSize.Medium, screenWidth, screenHeight)
        val large = resolveWindowSize(DialogSize.Large, screenWidth, screenHeight)

        assertTrue(small.first < medium.first && medium.first < large.first)
        assertTrue(small.second < medium.second && medium.second < large.second)
    }
}
