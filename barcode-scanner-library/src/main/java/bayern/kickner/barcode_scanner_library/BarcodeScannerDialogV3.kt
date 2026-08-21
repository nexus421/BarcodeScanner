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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
 * The camera is a process-wide resource ([androidx.camera.lifecycle.ProcessCameraProvider] is itself a
 * singleton), so only one [BarcodeScannerDialogV3] can actually use it at a time. Constructing a second instance
 * while one is still showing does not steal the camera from the first one - it immediately reports through
 * [onError] and shows a "busy" placeholder instead. This guard only protects against a second
 * [BarcodeScannerDialogV3] - it has no effect on [BarcodeScannerDialogV2], [BarcodeScannerContinuousDialog] or
 * [ImageCaptureDialog], which are unaware of it and can still claim the camera concurrently.
 *
 * This is a regular `class`, not a `data class` - constructing an instance has the side effect of immediately
 * showing a dialog (see [init]), so an accidental `.copy()` on an already-showing instance would show a second,
 * unwanted dialog and re-trigger [onError]/[onDismiss]. Making `.copy()`/`equals`/`hashCode` unavailable prevents
 * that footgun outright, at the cost of deviating from this library's usual `data class` convention.
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
class BarcodeScannerDialogV3(
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

    companion object {
        //Guards the process-wide camera resource against two instances of THIS class binding to it at the same
        //time. AtomicBoolean.compareAndSet is used instead of a plain @Volatile read-then-write, so claiming the
        //guard is a single atomic operation instead of two separate steps that could race.
        private val isAnyInstanceActive = AtomicBoolean(false)
    }

    private val dialog: AlertDialog
    private val rootLayout = (View.inflate(activity, R.layout.camera_dialog_layout_3, null) as ConstraintLayout).apply {
        if (titleLayout == null) findViewById<CardView>(R.id.headline).visibility = View.GONE
        else {
            val cardView = findViewById<CardView>(R.id.headline)
            val tv = findViewById<TextView>(R.id.tvHeadline)
            tv.text = titleLayout.title

            //Call function if not null to customize the views.
            titleLayout.customLayoutSettings?.let {
                try {
                    it(cardView, tv)
                } catch (e: Exception) {
                    onError("customLayoutSettings threw", e)
                }
            }
        }

        findViewById<ImageButton>(R.id.btn).apply {
            if (additionalButton != null) {
                visibility = View.VISIBLE
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

    //Created lazily so a denied camera permission never allocates an ML Kit scanner client. Uses the default
    //SYNCHRONIZED thread-safety mode since this can be first accessed either from the IO scan loop or from the
    //main-thread dismiss/cleanup path.
    private val scanner by lazy { BarcodeScanning.getClient(options) }

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

    //Atomically claims the flag for itself (if it's free) instead of a separate read now / write later, which
    //could otherwise let two near-simultaneous instances both see the guard as free.
    private val showingCameraPreview = permissionGranted && isAnyInstanceActive.compareAndSet(false, true)

    //Backstop in case setOnDismissListener never fires at all (e.g. the Activity gets destroyed/leaked without
    //properly dismissing the dialog first) - without this the guard could stay claimed forever. Only created when
    //this instance actually claimed the guard, and explicitly removed again in the dismiss listener below - so a
    //long-lived Activity showing many scan dialogs one after another doesn't keep accumulating stale observers.
    private val destroyObserver: LifecycleEventObserver? = if (showingCameraPreview) {
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) isAnyInstanceActive.set(false)
        }
    } else null

    init {
        dialog = when {
            permissionGranted.not() -> {
                onError("Camera permission is missing.", null)
                AlertDialog.Builder(activity).setTitle("Permission").setMessage("Camera permission is missing!").create()
            }

            showingCameraPreview.not() -> {
                onError("Another BarcodeScannerDialogV3 is already active - only one scanner can use the camera at a time.", null)
                AlertDialog.Builder(activity).setTitle("Scanner busy").setMessage("Another barcode scanner is already open.").create()
            }

            else -> {
                startCamera()
                AlertDialog.Builder(activity)
                    .setView(rootLayout)
                    .setCancelable(cancelable)
                    .create()
            }
        }

        dialog.setOnDismissListener {
            try {
                //Only the camera path allocated a camera/cameraProvider/scanner - skip cleanup entirely on the permission-denied/busy paths.
                if (cameraInitialized) {
                    search = false
                    scanJob?.cancel()
                }
            } catch (e: Exception) {
                onError("Error while cleaning up the barcode scanner", e)
            } finally {
                //Release the guard whenever this instance claimed it, regardless of whether binding the camera actually
                //succeeded. Runs in `finally` so a throwing cleanup above - or a throwing onError - can never leave the
                //flag stuck on true. The backstop observer is removed here too - once the dialog is properly dismissed
                //it's no longer needed, and leaving it registered would otherwise leak this instance off the Activity's
                //lifecycle for as long as the Activity itself stays alive.
                if (showingCameraPreview) {
                    isAnyInstanceActive.set(false)
                    destroyObserver?.let { activity.lifecycle.removeObserver(it) }
                }
                onDismiss?.invoke()
            }
        }

        destroyObserver?.let { activity.lifecycle.addObserver(it) }

        dialog.show()

        //The window size can only be applied reliably after show() - and only makes sense for the actual camera dialog.
        if (showingCameraPreview) applyDialogSize(dialogSize)
    }

    private fun applyDialogSize(size: DialogSize) {
        val window = dialog.window ?: return
        if (size == DialogSize.Fullscreen) {
            //Removes the dialog theme's rounded background/margins so the preview reaches every edge.
            window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
        //Uses the Activity's actual window/decor size instead of the raw display metrics, since the latter can be
        //larger than what's actually available to the Activity in split-screen/multi-window/foldable setups.
        val decorView = activity.window.decorView
        val screenWidth = decorView.width.takeIf { it > 0 } ?: activity.resources.displayMetrics.widthPixels
        val screenHeight = decorView.height.takeIf { it > 0 } ?: activity.resources.displayMetrics.heightPixels
        val (width, height) = resolveWindowSize(size, screenWidth, screenHeight)
        window.setLayout(width, height)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            //The dialog may already have been dismissed while the provider was still loading (e.g. a fast
            //back-tap) - don't bind a zombie camera that nothing will ever clean up again.
            if (!dialog.isShowing) return@addListener

            cameraProvider = try {
                cameraProviderFuture.get()
            } catch (exc: Exception) {
                onError("Failed to obtain the camera provider", exc)
                //A permanent provider failure means a successful scan can never happen anymore - the
                //cancelable = false "no way out except a successful scan" promise only makes sense for that case,
                //not for a technical defect, so close the dialog instead of leaving the user stuck.
                dialog.dismiss()
                return@addListener
            }

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
            } ?: run {
                dialog.dismiss()
                return@addListener
            }

            if (!dialog.isShowing) {
                //Dismissed in the exact window between the two isShowing checks - undo the bind we just did.
                cameraProvider.unbindAll()
                return@addListener
            }

            cameraInitialized = true
            //Only made visible/clickable now that `camera` is actually assigned - a tap before this point used to
            //be able to hit an uninitialized `camera` and crash.
            prepareTorch()
            if (torch == Torch.ForceOn) {
                isTorchOn = true
                camera.setTorch(true, btnTorch, onError)
            }

            scanJob = activity.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    while (search) {
                        val image = withContext(Dispatchers.Main) {
                            viewFinder.bitmap
                        }
                        //Awaiting the decode here (instead of firing it and moving on) is what gives this loop
                        //backpressure - the next frame is only grabbed once ML Kit is done with the current one.
                        val barcodes = image?.let { scanBarcode(it) }
                        if (barcodes != null) {
                            //Back on the main thread deliberately - suspendCancellableCoroutine resumes scanBarcode()
                            //on this loop's own IO dispatcher, not wherever ML Kit's listener happened to fire from.
                            withContext(Dispatchers.Main) {
                                if (search) {
                                    search = false
                                    //Guarded like every other caller-supplied hook in this class - an exception thrown
                                    //by the caller's onResult would otherwise leave lifecycleScope's coroutine with an
                                    //uncaught exception (no handler installed), crashing the whole app and skipping
                                    //dialog.dismiss() below.
                                    try {
                                        onResult(barcodes)
                                    } catch (e: Exception) {
                                        onError("onResult threw", e)
                                    }
                                    dialog.dismiss()
                                }
                            }
                        }
                        delay(timeToWaitAfterScan)
                    }
                } finally {
                    //Cleanup lives here now instead of in the dismiss listener, so it runs strictly *after* the loop
                    //above has actually stopped, not concurrently with a still in-flight scanner.process() call.
                    //NonCancellable so this block itself can't be torn down mid-cleanup by the very cancellation that
                    //triggered it, and Dispatchers.Main because unbindAll()/torch icon updates require the main thread.
                    withContext(Dispatchers.Main + NonCancellable) {
                        //Wrapped in try/catch so a throwing unbindAll()/close() can't crash the app through this
                        //coroutine's uncaught exception path - lifecycleScope installs no handler of its own for that.
                        try {
                            camera.setTorch(false, btnTorch, onError)
                            cameraProvider.unbindAll()
                            scanner.close()
                        } catch (e: Exception) {
                            onError("Error while cleaning up the barcode scanner", e)
                        }
                    }
                }
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun prepareTorch() {
        if (torch == Torch.Manual) {
            btnTorch.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    if (!cameraInitialized) return@setOnClickListener
                    isTorchOn = isTorchOn.not()
                    camera.setTorch(isTorchOn, btnTorch, onError)
                }
            }
        }
    }

    private suspend fun scanBarcode(bitmap: Bitmap): List<Barcode>? {
        val start = System.currentTimeMillis()
        val image = InputImage.fromBitmap(bitmap, 0)

        val barcodes = try {
            scanner.process(image).await()
        } catch (e: CancellationException) {
            throw e //Never swallow cancellation - the scan loop must actually stop when the job is cancelled.
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Failed scanning barcode, in ${System.currentTimeMillis() - start}ms", e)
            }
            return null
        }

        return resolveScanResult(barcodes)?.also {
            Log.d(TAG, "Detected ${it.size} barcode(s) in ${System.currentTimeMillis() - start}ms")
        }
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

/**
 * Suspends until [Task] completes, instead of registering fire-and-forget listeners. This is what lets the scan
 * loop wait for one frame's ML Kit decode to finish before it captures the next one, instead of piling up
 * overlapping [BarcodeScanning] calls when a device is too slow to keep up with [BarcodeScannerDialogV3]'s poll
 * interval. Not added as a dependency on `kotlinx-coroutines-play-services` since this one function is all that's needed.
 */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

internal fun Camera.setTorch(on: Boolean, imgBtn: ImageButton, onError: (msg: String, t: Throwable?) -> Unit) {
    try {
        cameraControl.enableTorch(on)
        imgBtn.setImageDrawable(
            ContextCompat.getDrawable(
                imgBtn.context,
                if (on) R.drawable.flashlight_on_24 else R.drawable.flashlight_off_24
            )
        )
    } catch (e: Exception) {
        onError("Error turning on torch", e)
    }
}
