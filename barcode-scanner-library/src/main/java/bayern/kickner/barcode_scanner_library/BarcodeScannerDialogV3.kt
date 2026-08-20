package bayern.kickner.barcode_scanner_library

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BarcodeScannerDialogV3"

/**
 * Displays a camera preview inside a dialog for barcode scanning using Google's ML Kit.
 * Based on [BarcodeScannerDialogV2] - same permission handling, torch handling and single-shot dismiss behaviour.
 * @see https://codelabs.developers.google.com/codelabs/camerax-getting-started
 *
 * The key difference to [BarcodeScannerDialogV2]: a frame is accepted as soon as ML Kit detects at least one
 * barcode (not exactly one), and every barcode found in that frame is forwarded through [onResult] - not just a
 * single decoded string. ML Kit's [Barcode] type does not expose any confidence/score per barcode, so there is
 * nothing reliable to rank a "best match" by. The list is therefore handed through exactly in ML Kit's own
 * detection order, unmodified - callers who need more than [Barcode.getRawValue] (e.g. [Barcode.getBoundingBox]
 * or [Barcode.getCornerPoints] to draw a marker on the image) already have everything they need on each entry.
 *
 * @param activity used to display this dialog
 * @param barcodeFormats Use [Barcode] format constants to search for. Defaults to [Barcode.FORMAT_ALL_FORMATS].
 * Duplicate values are ignored, an empty list falls back to [Barcode.FORMAT_ALL_FORMATS].
 * @param titleLayout Displays a title at the top. If null, this won't be visible. Custom settings through [TitleLayout]
 * @param cancelable default to true. If false, the user can't dismiss this dialog without a successful scan
 * @param torch choose the settings through [Torch] defaults to [Torch.Manual]
 * @param dialogSize controls how large the dialog window is, see [DialogSize]. Defaults to [DialogSize.Fullscreen].
 * Whichever size is chosen, the whole visible preview is what gets scanned - there is no smaller "scan window" inside it.
 * @param additionalButton if not null, a button will be displayed on the bottom left, based on this settings
 * @param onError any error or failed barcode scan will be send to this callback. Defaults to log through [Log.e]
 * @param onDismiss will be called after everything was cleaned up and the dialog will be dismissed finally
 * @param onResult every barcode ML Kit detected in the accepted frame, in ML Kit's own order
 */
data class BarcodeScannerDialogV3(
    private val activity: ComponentActivity,
    private val barcodeFormats: List<Int> = listOf(Barcode.FORMAT_ALL_FORMATS),
    private val titleLayout: TitleLayout? = TitleLayout(),
    private val cancelable: Boolean = true,
    private val torch: Torch = Torch.Manual,
    private val dialogSize: DialogSize = DialogSize.Fullscreen,
    private val additionalButton: ButtonSettings? = null,
    private val onError: ((msg: String, t: Throwable?) -> Unit) = { s, t -> Log.e(TAG, s, t) },
    private val onDismiss: (() -> Unit)? = null,
    private val onResult: (barcodes: List<Barcode>) -> Unit
) {

    private val dialog: AlertDialog
    private val rootLayout = (View.inflate(activity, R.layout.camera_dialog_layout_3, null) as ConstraintLayout).apply {
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

    private val options = run {
        val formats = resolveBarcodeFormats(barcodeFormats)
        BarcodeScannerOptions.Builder().setBarcodeFormats(formats[0], *formats.drop(1).toIntArray()).build()
    }

    //Created lazily so a denied camera permission never allocates an ML Kit scanner client.
    private val scanner by lazy(LazyThreadSafetyMode.NONE) { BarcodeScanning.getClient(options) }

    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private var scanJob: Job? = null

    @Volatile
    private var search = true
    private val timeToWaitAfterScan = 100L

    @Volatile
    private var isTorchOn = false

    @Volatile
    private var cameraInitialized = false

    private val permissionGranted = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PERMISSION_GRANTED

    init {
        dialog = if (!permissionGranted) {
            onError("Camera permission is missing.", null)
            AlertDialog.Builder(activity).setTitle("Permission").setMessage("Camera permission is missing!").create()
        } else {
            startCamera()
            prepareTorch()
            AlertDialog.Builder(activity)
                .setView(rootLayout)
                .setCancelable(cancelable)
                .create()
        }

        dialog.setOnDismissListener {
            //Only the camera path allocated a camera/cameraProvider/scanner - skip cleanup entirely on the permission-denied path.
            if (cameraInitialized) {
                try {
                    search = false
                    scanJob?.cancel()
                    camera.setTorch(false, btnTorch)
                    cameraProvider.unbindAll()
                    scanner.close()
                } catch (e: Exception) {
                    onError("Error while cleaning up the barcode scanner", e)
                }
            }
            onDismiss?.invoke()
        }

        dialog.show()

        //The window size can only be applied reliably after show() - and only makes sense for the actual camera dialog.
        if (permissionGranted) applyDialogSize(dialogSize)
    }

    private fun applyDialogSize(size: DialogSize) {
        val window = dialog.window ?: return
        if (size == DialogSize.Fullscreen) {
            //Removes the dialog theme's rounded background/margins so the preview reaches every edge.
            window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
        val metrics = activity.resources.displayMetrics
        val (width, height) = resolveWindowSize(size, metrics.widthPixels, metrics.heightPixels)
        window.setLayout(width, height)
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

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            camera = try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, cameraSelector, preview)
            } catch (exc: Exception) {
                onError("Use case binding failed", exc)
                null
            } ?: return@addListener

            cameraInitialized = true
            if (torch == Torch.ForceOn) camera.setTorch(true, btnTorch)

            scanJob = activity.lifecycleScope.launch(Dispatchers.IO) {
                while (search) {
                    val image = withContext(Dispatchers.Main) {
                        viewFinder.bitmap
                    }
                    if (image != null) {
                        scanBarcode(image) { barcodes ->
                            if (search) {
                                search = false
                                onResult(barcodes)
                                dialog.dismiss()
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

    private fun scanBarcode(bitmap: Bitmap, result: (List<Barcode>) -> Unit) {
        val start = System.currentTimeMillis()
        val image = InputImage.fromBitmap(bitmap, 0)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                resolveScanResult(barcodes)?.let {
                    Log.d(TAG, "Detected ${it.size} barcode(s) in ${System.currentTimeMillis() - start}ms")
                    result(it)
                }
            }
            .addOnFailureListener { onError("Failed scanning barcode, in ${System.currentTimeMillis() - start}ms", it) }
    }

    fun dismiss() {
        dialog.dismiss()
    }

}

/**
 * [BarcodeScannerOptions.Builder.setBarcodeFormats] requires at least one format and would throw on an empty
 * vararg. Deduplicates the requested formats and falls back to [Barcode.FORMAT_ALL_FORMATS] when [formats] is
 * empty, so an accidental `emptyList()` can't crash the dialog during construction.
 */
internal fun resolveBarcodeFormats(formats: List<Int>): IntArray {
    val distinct = formats.distinct()
    return if (distinct.isEmpty()) intArrayOf(Barcode.FORMAT_ALL_FORMATS) else distinct.toIntArray()
}

/**
 * ML Kit's [Barcode] does not expose a confidence/score, so there is nothing to rank a "best match" by.
 * This intentionally returns [barcodes] unmodified - callers should treat ML Kit's detection order as-is, not as
 * a ranking by likelihood. Returns null when nothing was detected, signalling "keep scanning".
 */
internal fun resolveScanResult(barcodes: List<Barcode>): List<Barcode>? = barcodes.takeIf { it.isNotEmpty() }

/**
 * Turns a [DialogSize] into the pixel width/height to pass to [android.view.Window.setLayout], given the actual
 * screen dimensions. [DialogSize.Fullscreen] always resolves to [ViewGroup.LayoutParams.MATCH_PARENT] rather than
 * a computed 100% pixel value, matching what [android.view.Window.setLayout] itself expects for "fill everything".
 */
internal fun resolveWindowSize(size: DialogSize, screenWidthPx: Int, screenHeightPx: Int): Pair<Int, Int> {
    if (size == DialogSize.Fullscreen) return ViewGroup.LayoutParams.MATCH_PARENT to ViewGroup.LayoutParams.MATCH_PARENT
    val width = (screenWidthPx * size.widthFraction).toInt()
    val height = (screenHeightPx * size.heightFraction).toInt()
    return width to height
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
        Log.e(TAG, "Error turning on torch")
    }
}
