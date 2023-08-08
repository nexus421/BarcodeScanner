package bayern.kickner.barcode_scanner_library

import android.Manifest
import android.app.AlertDialog
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Zeigt eine Kamera-Vorschau in einem Dialog an
 * In Kombination mit BarcodeScanner kann hier ein Barcode eingelesen werden.
 *
 * Der Dialog wird sofort von selbst gestartet. Die Berechtigung wird dabei selbstständig abgefragt.
 *
 * @see https://codelabs.developers.google.com/codelabs/camerax-getting-started
 * @author MK
 *
 * @param ignorePermissionCheck If you are using this inside compose, don't handle permission-check here (set to true!). Do it on your own. Otherwise your App will crash, when the permission is not given to your App!
 * @param barcode Callback which will be called after an barcode was detected
 * @param continuousScanSettings Scanns continually until finish or back is pressed, if not null
 * @param buttonSettings Settings for the FAB on the bottom right. If set (not null) you can specify custom actions. Otherwise the Button is no visible
 * @param options Set here the types of codes you want to search for. Default: TYPE_PRODUCT, FORMAT_QR_CODE, TYPE_ISBN, FORMAT_EAN_13 and FORMAT_EAN_8
 * @param torch set the flashlight mode here. Default is off.
 *
 * ToDo: Hier könnte man auch über Parameter und dem zugehörigen ML Kit einen Text einscannen. Datum automatisch erkennen! xx.xx.xxxx
 */
class BarcodeScannerDialog(
    private val activity: ComponentActivity,
    private val options: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_ALL_FORMATS
        )
        .build(),
    private val title: String = "Barcode scannen",
    private val missingPermissionText: String = "Kamera-Berechtigung verweigert.",
    private val ignorePermissionCheck: Boolean = true,
    private val buttonSettings: ButtonSettings? = null,
    private val continuousScanSettings: ContinuousScanSettings? = null,
    private val torch: Torch = Torch.Off,
    private val barcode: (String) -> Unit
) {

    private lateinit var viewFinder: PreviewView
    lateinit var dialog: AlertDialog
    private lateinit var rootView: ConstraintLayout
    private val search = AtomicBoolean(true)
    private val timeToWaitAfterScan: Long = continuousScanSettings?.timeToWaitBetweenScans?.toLong() ?: 100


    init {
        if (ignorePermissionCheck) {
            initAfterPermissionCheck()
        } else {

            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
                initAfterPermissionCheck()
            } else {
                val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (it) initAfterPermissionCheck()
                    else AlertDialog.Builder(activity).setTitle("Permission").setMessage(missingPermissionText)
                        .setPositiveButton("Ok") { d, _ -> d.dismiss() }.show()
                }
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun initAfterPermissionCheck() {
        //If you have an xml with the same name, your app will crash. Reason: This class will use your Layout, not this one.
        rootView = View.inflate(activity, R.layout.camera_dialog_layout, null) as ConstraintLayout
        rootView.findViewById<TextView>(R.id.tvHeadline).text = title
        viewFinder = rootView.findViewById(R.id.viewFinder)

        prepareFab()
        prepareTorch()

        dialog = AlertDialog.Builder(activity)
            .setView(rootView)
            .setCancelable(true)
            .create()

        dialog.setOnDismissListener {
            search.set(false)
        }

        startCamera()
        show()
    }

    private fun prepareFab() {
        if (buttonSettings == null) return

        val imgBtn = rootView.findViewById<ImageButton>(R.id.fab)
        imgBtn.setImageDrawable(buttonSettings.fabIcon)
        imgBtn.setOnClickListener {
            buttonSettings.onClick(dialog)
        }
        imgBtn.visibility = View.VISIBLE

    }

    private var torchOn = false

    private fun prepareTorch() {
        when (torch) {
            Torch.ForceOn -> return //Directly handled inside startCamera()
            Torch.Off -> return
            Torch.Manual -> {
                rootView.findViewById<ImageButton>(R.id.fabTorch).apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        torchOn = torchOn.not()
                        try {
                            camera.cameraControl.enableTorch(torchOn)
                        } catch (e: Exception) {
                            Log.e("Torch", "Error switching torch")
                        }
                    }
                }
            }
        }
    }

    private lateinit var camera: Camera

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)

                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            camera = try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e("BarcodeScannerDialog", "Use case binding failed", exc)
                null
            } ?: return@addListener

            if (torch == Torch.ForceOn) {
                try {
                    camera.cameraControl.enableTorch(true)
                } catch (e: Exception) {
                    Log.e("Torch", "Error turning on torch")
                }
            }

            activity.lifecycleScope.launch(Dispatchers.IO) {
                delay(200)
                while (search.get()) {
                    withContext(Dispatchers.Main) {
                        val image = viewFinder.bitmap
                        if (image != null) {
                            scanBarcode(image) { scannedBarcode ->
                                if (scannedBarcode.isNotBlank() && search.get()) {
                                    if (continuousScanSettings != null) {
                                        continuousScanSettings.checkInputAndReturnIfOk(scannedBarcode)?.let {
                                            barcode(it)
                                        }
                                    } else {
                                        search.set(false)
                                        barcode(scannedBarcode)
                                        dismiss()
                                    }
                                }
                            }
                        }
                    }
                    delay(timeToWaitAfterScan)
                }
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun scanBarcode(bitmap: Bitmap, result: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient(options)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.size == 1) {
                    val barcode = barcodes[0]
                    barcode.rawValue?.let { result(it) }
                    Log.d("BarcodeScannerDialog", "Barcode: Type = ${barcode.valueType}, Value = ${barcode.rawValue}")
                }
            }
            .addOnFailureListener { Log.e("BarcodeScannerDialog", "#scanBarcode", it) }
    }

    private fun show() = dialog.show()
    fun dismiss() = dialog.dismiss()

    private fun scanForDate() {
        TODO("Text scannen mit ML Kit. Nach Datum suchen im Format dd.mm.yyyy")
    }
}