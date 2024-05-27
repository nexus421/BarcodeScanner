package bayern.kickner.barcode_scanner_library

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.view.Surface
import androidx.camera.core.ImageCapture
import java.io.ByteArrayOutputStream

enum class Flashlight(val type: Int) {
    On(ImageCapture.FLASH_MODE_ON), Off(ImageCapture.FLASH_MODE_OFF), Auto(ImageCapture.FLASH_MODE_AUTO)
}

enum class CaptureMode(val type: Int) {
    MinimalLatency(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY), MaximumQuality(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
}

enum class CameraRotation(val type: Int) {
    Rotate0(Surface.ROTATION_0), Rotate90(Surface.ROTATION_90), Rotate180(Surface.ROTATION_180), Rotate270(Surface.ROTATION_270)

}

sealed class ImageCaptureResult<T>(val outputFileOptions: ImageCapture.OutputFileOptions, val onResult: (T) -> Boolean) {

    /**
     * With this [ImageCapture.OutputFileOptions] the image will not be written to the storage. You will only receive the bytearray with the image.
     */
    class ByteArray(private val out: ByteArrayOutputStream = ByteArrayOutputStream(), onResult: (kotlin.ByteArray) -> Boolean) :
        ImageCaptureResult<kotlin.ByteArray>(ImageCapture.OutputFileOptions.Builder(out).build(), onResult) {
        fun readStreamAndClose(): kotlin.ByteArray = out.use { it.toByteArray() }
    }

    /**
     * Stores the image at [destinationFile]
     */
    class File(val destinationFile: java.io.File, onResult: (java.io.File) -> Boolean) :
        ImageCaptureResult<java.io.File>(ImageCapture.OutputFileOptions.Builder(destinationFile).build(), onResult)

    /**
     * Receive an URI with the image.
     */
    class Uri(
        contentResolver: ContentResolver,
        saveCollectionUri: android.net.Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues: ContentValues,
        onResult: (android.net.Uri) -> Boolean
    ) : ImageCaptureResult<android.net.Uri>(
        ImageCapture.OutputFileOptions.Builder(contentResolver, saveCollectionUri, contentValues).build(),
        onResult
    ) {
        companion object {
            /**
             * Simple example for content values you can use.
             */
            fun createContentValues(name: String, mimeType: String = "image/jpeg") = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
                }
            }
        }
    }

}