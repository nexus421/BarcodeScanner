package bayern.kickner.barcode_scanner_library

import android.view.Surface
import androidx.camera.core.ImageCapture

enum class Flashlight(val type: Int) {
    On(ImageCapture.FLASH_MODE_ON), Off(ImageCapture.FLASH_MODE_OFF), Auto(ImageCapture.FLASH_MODE_AUTO)
}

enum class CaptureMode(val type: Int) {
    MinimalLatency(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY), MaximumQuality(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
}

enum class CameraRotation(val type: Int) {
    Rotate0(Surface.ROTATION_0), Rotate90(Surface.ROTATION_90), Rotate180(Surface.ROTATION_180), Rotate270(Surface.ROTATION_270)

}