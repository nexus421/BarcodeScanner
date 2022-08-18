package bayern.kickner.barcode_scanner_library

import android.Manifest
import android.app.AlertDialog
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
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
 *
 * ToDo: Hier könnte man auch über Parameter und dem zugehörigen ML Kit einen Text einscannen. Datum automatisch erkennen! xx.xx.xxxx
 */
class BarcodeScannerDialog (
    private val activity: ComponentActivity,
    private val options: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.TYPE_PRODUCT,
            Barcode.FORMAT_QR_CODE,
            Barcode.TYPE_ISBN,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8
        )
        .build(),
    title: String = "Barcode scannen",
    val barcode: (String) -> Unit
) {

    private lateinit var viewFinder: PreviewView
    private lateinit var dialog: AlertDialog
    private lateinit var rootView: View

    init {
        val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if(it) {
                rootView = View.inflate(activity, R.layout.camera_dialog_layout, null)
                rootView.findViewById<TextView>(R.id.tvHeadline).text = title

                dialog = AlertDialog.Builder(activity)
                    .setView(rootView)
                    .setCancelable(true)
                    .create()

                viewFinder = rootView.findViewById(R.id.viewFinder)

                startCamera()
                show()
            } else {
                AlertDialog.Builder(activity).setTitle("Permission").setMessage("Kamera-Berechtigung verweigert.").setPositiveButton("Ok") {d, _ -> d.dismiss() }.show()
            }
        }

        launcher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera (AppCombatActivity ist hier zwingend nötig!)
                cameraProvider.bindToLifecycle(activity, cameraSelector, preview)

            } catch (exc: Exception) {
                Log.e("BarcodeScannerDialog", "Use case binding failed", exc)
            }

            activity.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    delay(200)
                    val search = AtomicBoolean(true)
                    while (search.get()) {
                        withContext(Dispatchers.Main) {
                            val image = viewFinder.bitmap
                            if (image != null) {
                                scanBarcode(image) {
                                    if (!it.isNullOrBlank() && search.get()) {
                                        search.set(false)
                                        barcode(it)
                                        dismiss()
                                    }
                                }

                            }
                        }
                        delay(200)
                    }
                }
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun scanBarcode(bitmap: Bitmap, result: (String?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient(options)

        scanner.process(image)
            .addOnSuccessListener {
                if (it.size == 1) {
                    val barcode = it[0]
                    result(barcode.rawValue)
                    Log.d(
                        "BarcodeScannerDialog",
                        "Barcode: Type = ${barcode.valueType}, Value = ${barcode.rawValue}"
                    )
                }
            }
            .addOnFailureListener { Log.e("BarcodeScannerDialog", "#scanBarcode", it) }
    }

    private fun show() = dialog.show()
    fun dismiss() = dialog.dismiss()

    private fun scanForDate(){
        TODO("Text scannen mit ML Kit. Nach Datum suchen im Format dd.mm.yyyy")
    }
}