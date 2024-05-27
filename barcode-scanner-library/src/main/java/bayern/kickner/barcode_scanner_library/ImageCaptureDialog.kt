package bayern.kickner.barcode_scanner_library

import android.Manifest
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

/**
 * Simple Dialog to Display the current view form the back main camera. Simply click the camera button to take a picture.
 *
 * WARNING: KNOWN BUG - Image always rotated by 90°.
 *
 * @param flashlight set the flashlight mode. Default is off.
 * @param captureMode set this to prioritize quality or speed.
 * @param jpegQuality set the jpeg quality. Default is 95. Higher results in better image quality.
 * @param rotation if null, the current display state will be used. You can force an orientation state through [CameraRotation].
 * @param onError called on any error
 * @param imageCaptureResult select the desired result to receive the image.
 */
class ImageCaptureDialog<T>(
    private val activity: ComponentActivity,
    private val titleLayout: TitleLayout? = null,
    cancelable: Boolean = true,
    private val flashlight: Flashlight = Flashlight.Off,
    private val captureMode: CaptureMode = CaptureMode.MinimalLatency,
    @androidx.annotation.IntRange(from = 1L, to = 100L) private val jpegQuality: Int = 95,
    rotation: CameraRotation? = null,
    private val onError: ((msg: String, t: Throwable?) -> Unit) = { s, t -> Log.e("ImageCaptureDialog", s, t) },
    private val imageCaptureResult: ImageCaptureResult<T>
) {

    private val dialog: AlertDialog
    private val rootLayout = (View.inflate(activity, R.layout.image_capture_dialog_layout, null) as ConstraintLayout)
    private val viewFinder = rootLayout.findViewById<PreviewView>(R.id.viewFinder)
    private val btnFlashlight = rootLayout.findViewById<ImageButton>(R.id.btnTorch)
    private val btnTakePicture = rootLayout.findViewById<ImageButton>(R.id.btn)
    private lateinit var cameraProvider: ProcessCameraProvider
    private val imageCapture = ImageCapture.Builder().apply {
        setFlashMode(flashlight.type)
        setCaptureMode(captureMode.type)
        setJpegQuality(jpegQuality)
    }.build()

    init {
        dialog = if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED) {
            onError("Camera permission is missing.", null)
            AlertDialog.Builder(activity).setTitle("Permission").setMessage("Camera permission is missing!").create()
        } else {
            prepareFlashlight()
            startCamera()
            AlertDialog.Builder(activity)
                .setView(rootLayout)
                .setCancelable(cancelable)
                .create()
        }

        rotation?.type?.let {
            imageCapture.targetRotation = it
        }

        rootLayout.findViewById<CardView>(R.id.headline).apply {
            if (titleLayout == null) {
                visibility = View.GONE
                return@apply
            } else {
                val tv = findViewById<TextView>(R.id.tvHeadline)
                tv.text = titleLayout.title

                //Call function if not null to customize the views.
                titleLayout.customLayoutSettings?.let {
                    it(this, tv)
                }
            }
        }

        btnTakePicture.setOnClickListener {
            imageCapture.takePicture(
                imageCaptureResult.outputFileOptions,
                ContextCompat.getMainExecutor(activity),
                object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val dismiss = when (imageCaptureResult) {
                        is ImageCaptureResult.ByteArray -> imageCaptureResult.onResult(imageCaptureResult.readStreamAndClose())
                        is ImageCaptureResult.File -> imageCaptureResult.onResult(imageCaptureResult.destinationFile)
                        is ImageCaptureResult.Uri -> imageCaptureResult.onResult(outputFileResults.savedUri!!)
                    }
                    if (dismiss) dialog.dismiss()
                }

                override fun onError(exception: ImageCaptureException) {
                    onError("Error capturing image", exception)
                }

            })
        }

        dialog.setOnDismissListener {
            try {
                //Aufräumen und Kamera wieder freigeben.
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                onError("", e)
            }
        }

        dialog.show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .apply {
                    setSurfaceProvider(viewFinder.surfaceProvider)
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (exc: Exception) {
                onError("Use case binding failed", exc)
                null
            } ?: return@addListener
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun prepareFlashlight() {
        //Setzen des initialen Blitzes
        when (flashlight) {
            Flashlight.On -> {
                btnFlashlight.setImageResource(R.drawable.baseline_flash_on_24)
                btnFlashlight.tag = Flashlight.On
                imageCapture.flashMode = Flashlight.On.type
            }

            Flashlight.Off -> {
                btnFlashlight.setImageResource(R.drawable.baseline_flash_off_24)
                btnFlashlight.tag = Flashlight.Off
                imageCapture.flashMode = Flashlight.Off.type
            }

            Flashlight.Auto -> {
                btnFlashlight.setImageResource(R.drawable.baseline_flash_auto_24)
                btnFlashlight.tag = Flashlight.Auto
                imageCapture.flashMode = Flashlight.Auto.type
            }
        }

        //Wechsel des blitzes
        btnFlashlight.setOnClickListener {
            val flashlightMode = it.tag as? Flashlight ?: flashlight

            when (flashlightMode) {
                Flashlight.On -> {
                    btnFlashlight.setImageResource(R.drawable.baseline_flash_off_24)
                    it.tag = Flashlight.Off
                    imageCapture.flashMode = Flashlight.Off.type
                }

                Flashlight.Off -> {
                    btnFlashlight.setImageResource(R.drawable.baseline_flash_auto_24)
                    it.tag = Flashlight.Auto
                    imageCapture.flashMode = Flashlight.Auto.type
                }

                Flashlight.Auto -> {
                    btnFlashlight.setImageResource(R.drawable.baseline_flash_on_24)
                    it.tag = Flashlight.On
                    imageCapture.flashMode = Flashlight.On.type
                }
            }
        }
    }

}