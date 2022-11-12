package bayern.kickner.barcode_scanner_library

import android.Manifest
import android.app.AlertDialog
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
 *
 * ToDo: Hier könnte man auch über Parameter und dem zugehörigen ML Kit einen Text einscannen. Datum automatisch erkennen! xx.xx.xxxx
 */
class BarcodeScannerDialog(
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
    private val title: String = "Barcode scannen",
    private val missingPermissionText: String = "Kamera-Berechtigung verweigert.",
    private val ignorePermissionCheck: Boolean = false,
    private val barcode: (String) -> Unit
) {

    private lateinit var viewFinder: PreviewView
    private lateinit var dialog: AlertDialog
    private lateinit var rootView: LinearLayoutCompat

    init {
        if (ignorePermissionCheck) initAfterPermissionCheck()

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
            initAfterPermissionCheck()
        } else {
            val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) initAfterPermissionCheck()
                else AlertDialog.Builder(activity).setTitle("Permission").setMessage(missingPermissionText).setPositiveButton("Ok") { d, _ -> d.dismiss() }.show()
            }
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun initAfterPermissionCheck() {
        //If you have an xml with the same name, your app will crash. Reason: This class will use your Layout, not this one.
        rootView = View.inflate(activity, R.layout.camera_dialog_layout, null) as LinearLayoutCompat
        rootView.findViewById<TextView>(R.id.tvHeadline).text = title
        viewFinder = rootView.findViewById(R.id.viewFinder)

        dialog = AlertDialog.Builder(activity)
            .setView(rootView)
            .setCancelable(true)
            .create()

        startCamera()
        show()
    }

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

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e("BarcodeScannerDialog", "Use case binding failed", exc)
            }

            activity.lifecycleScope.launch(Dispatchers.IO) {
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

    private fun scanForDate() {
        TODO("Text scannen mit ML Kit. Nach Datum suchen im Format dd.mm.yyyy")
    }
}