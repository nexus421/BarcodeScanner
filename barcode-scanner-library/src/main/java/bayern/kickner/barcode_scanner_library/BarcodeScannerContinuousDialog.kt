package bayern.kickner.barcode_scanner_library

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Displays a camera preview inside an simple dialog inlcuding the Google ML Kit for high quality barcode scanning.
 * Based on Codelabs.
 * @see https://codelabs.developers.google.com/codelabs/camerax-getting-started
 *
 * @param activity used to display this dialog
 * @param barcodeFormats Use [Barcode] for barcodes to search for. Defaults to [Barcode.FORMAT_ALL_FORMATS]
 * @param titleLayout Displays a title at the top. If null, this won't be visible. Custom settings through [TitleLayout]
 * @param cancelable default to true. If false, the user can't dismiss this button without a successful scan
 * @param torch choose the settings through [Torch] defaults to [Torch.Manual]
 * @param additionalButton if not null, a button will be displayed on the bottom left, based on this settings
 * @param vibrateOnScanned if true, the device will vibrate for 200ms on an scan result
 * @param onError any error or failed barcode scan will be send to this callback. Defaults to log through [Log.e]
 * @param onResult the found barcode will be send to this callback as a simple string. As long as you return false, the dialog will not be closed. Hint: you may delay the returning.
 *
 * Hint: If you want a single recognition like [BarcodeScannerDialogV2] return true after the first result within [onResult]
 */
data class BarcodeScannerContinuousDialog(
    private val activity: ComponentActivity,
    private val barcodeFormats: List<Int> = listOf(Barcode.FORMAT_ALL_FORMATS),
    private val titleLayout: TitleLayout? = TitleLayout(),
    private val cancelable: Boolean = true,
    private val torch: Torch = Torch.Manual,
    private val additionalButton: ButtonSettings? = null,
    private val vibrateOnScanned: Boolean = true,
    private val onError: ((msg: String, t: Throwable?) -> Unit) = { s, t ->
        Log.e(
            "BarcodeScannerContinuousDialog",
            s,
            t
        )
    },
    private val onResult: (barcode: String) -> Boolean
) {

    private val dialog: AlertDialog
    private val rootLayout = (View.inflate(activity, R.layout.camera_dialog_layout_2, null) as ConstraintLayout).apply {
        if (titleLayout == null) findViewById<CardView>(R.id.headline).visibility = View.GONE
        else {
            val cardView = findViewById<CardView>(R.id.headline)
            val tv = findViewById<TextView>(R.id.tvHeadline)
            tv.text = titleLayout.title

            //Call function if not null to customize the views.
            titleLayout.customLayoutSettings?.let {
                it(cardView, tv)
            }
        }

        findViewById<ImageButton>(R.id.btn).apply {
            if (additionalButton != null) {
                this.setImageDrawable(additionalButton.btnIcon)
                setOnClickListener { additionalButton.onClick() }
            }
        }
    }
    private val viewFinder = rootLayout.findViewById<PreviewView>(R.id.viewFinder)
    private val btnTorch = rootLayout.findViewById<ImageButton>(R.id.btnTorch)

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(barcodeFormats.first(), *barcodeFormats.drop(1).toIntArray())
        .build()
    private lateinit var camera: Camera

    @Volatile
    private var search = true
    private val timeToWaitAfterScan = 100L

    private val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    @Volatile
    private var isTorchOn = false

    init {
        dialog = if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
            onError("Camera permission is missing.", null)
            AlertDialog.Builder(activity).setTitle("Permission").setMessage("Camera permission is missing!").create()
        } else {
            startCamera()
            prepareTorch()
            AlertDialog.Builder(activity)
                .setView(rootLayout)
                .setCancelable(cancelable)
                .setOnDismissListener {
                    search = false
                    camera.setTorch(false, btnTorch)
                }.create()
        }

        dialog.show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .apply {
                    setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            camera = try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, cameraSelector, preview)

            } catch (exc: Exception) {
                onError("Use case binding failed", exc)
                null
            } ?: return@addListener

            if (torch == Torch.ForceOn) camera.setTorch(true, btnTorch)

            activity.lifecycleScope.launch(Dispatchers.IO) {
                while (search) {
                    val image = withContext(Dispatchers.Main) {
                        viewFinder.bitmap
                    }
                    if (image != null) {
                        scanBarcode(image) { scannedBarcode ->
                            if (scannedBarcode.isNotBlank() && search) {
                                search = false
                                if (vibrateOnScanned) {
                                    if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                    else vibrator.vibrate(200)
                                }
                                if (onResult(scannedBarcode)) return@scanBarcode dialog.dismiss()
                                search = true
                            }
                        }
                    }
                    delay(timeToWaitAfterScan)
                }
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun prepareTorch() {
        if (torch == Torch.Manual) {
            btnTorch.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    isTorchOn = isTorchOn.not()
                    camera.setTorch(isTorchOn, btnTorch)
                }
            }
        }
    }

    private fun scanBarcode(bitmap: Bitmap, result: (String) -> Unit) {
        val start = System.currentTimeMillis()
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient(options)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.size == 1) {
                    val barcode = barcodes[0]
                    barcode.rawValue?.let { result(it) }
                    Log.d(
                        "BarcodeScannerDialog",
                        "Barcode: Type = ${barcode.valueType}, Value = ${barcode.rawValue}, in ${System.currentTimeMillis() - start}ms"
                    )
                }
            }
            .addOnFailureListener { onError("Failed scanning barcode, in ${System.currentTimeMillis() - start}ms", it) }
    }

    fun dismiss() {
        dialog.dismiss()
    }

}

private fun Camera.setTorch(on: Boolean, imgBtn: ImageButton) {
    try {
        cameraControl.enableTorch(on)
        imgBtn.setImageDrawable(
            ContextCompat.getDrawable(
                imgBtn.context,
                if (on) R.drawable.flashlight_on_24 else R.drawable.flashlight_off_24
            )
        )
    } catch (e: Exception) {
        Log.e("Torch", "Error turning on torch")
    }
}