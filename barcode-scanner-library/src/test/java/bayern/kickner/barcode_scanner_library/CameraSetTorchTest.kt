package bayern.kickner.barcode_scanner_library

import android.widget.ImageButton
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * [Camera.setTorch] used to only [android.util.Log.e] a failed [CameraControl.enableTorch] call, so callers had no
 * way to react to a broken torch (e.g. show it to the user). This test locks in that a failure is now reported
 * through the dialog's `onError` callback instead of being swallowed into a log line only.
 */
class CameraSetTorchTest {

    @Test
    fun `a failing enableTorch call is reported through onError, not just logged`() {
        val cameraControl = mock(CameraControl::class.java)
        val failure = IllegalStateException("torch hardware unavailable")
        `when`(cameraControl.enableTorch(true)).thenThrow(failure)
        val camera = mock(Camera::class.java)
        `when`(camera.cameraControl).thenReturn(cameraControl)
        val imgBtn = mock(ImageButton::class.java)

        var reportedMessage: String? = null
        var reportedThrowable: Throwable? = null
        camera.setTorch(true, imgBtn) { msg, t ->
            reportedMessage = msg
            reportedThrowable = t
        }

        assertSame(failure, reportedThrowable)
        assert(reportedMessage != null && reportedMessage!!.isNotBlank())
    }
}
