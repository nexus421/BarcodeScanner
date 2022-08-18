package bayern.kickner.barcode_scanner_library

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Zeigt eine Kamera-Vorschau in einer eigenen Activity an
 * In Kombination mit BarcodeScanner kann hier ein Barcode eingelesen werden.
 *
 * TODO....
 * @author MK
 */
private class BarcodeScannerActivity : AppCompatActivity() {

    object INTENT {
        const val TITLE = "title"
        const val RESULT = "result"

        /**
         * Erzeugt ein Intent, mit dem einfach diese Activity aufgerufen werden kann.
         *
         * @param activity Aufrufende Activity
         * @param title Nachricht, die angezeigt werden soll
         */
        fun getIntent(activity: Activity, title: String = ""): Intent {
            val i = Intent(activity, BarcodeScannerActivity::class.java)
            i.putExtra(TITLE, title)
            return i
        }
    }

    private lateinit var viewFinder: PreviewView
    private val options: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.TYPE_PRODUCT,
            Barcode.FORMAT_QR_CODE,
            Barcode.TYPE_ISBN,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8
        )
        .build()
    private lateinit var title: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)
        viewFinder = findViewById(R.id.viewFinder)
        title = intent.getStringExtra(INTENT.TITLE) ?: ""

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PermissionChecker.PERMISSION_GRANTED){
            AlertDialog.Builder(this).setTitle("Permission").setMessage("Kamera-Berechtigung verweigert.").setPositiveButton("Ok") { d, _ ->
                d.dismiss()
                finish()
            }.show()
            return
        }
        supportActionBar?.hide()

        findViewById<TextView>(R.id.tvMsg).text = title

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

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
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)

            } catch (exc: Exception) {
                Log.e("BarcodeScannerDialog", "Use case binding failed", exc)
            }

            lifecycleScope.launch(Dispatchers.IO) {
                delay(200)
                val search = AtomicBoolean(true)
                while (search.get()) {
                    withContext(Dispatchers.Main) {
                        val image = viewFinder.bitmap
                        if (image != null) {
                            scanBarcode(image) {
                                if (!it.isNullOrBlank() && search.get()) {
                                    search.set(false)
                                    prepareAndFinish(it)
                                }
                            }

                        }
                    }
                    delay(200)
                }
            }


        }, ContextCompat.getMainExecutor(this))
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
            .addOnFailureListener {
                Log.e(
                    "BarcodeScannerDialog",
                    "BarcodeScannerActivity#scanBarcode",
                    it
                )
            }
    }

    private fun prepareAndFinish(data: String) {
        val i = Intent()
        i.putExtra(INTENT.RESULT, data)
        setResult(RESULT_OK, i)
        finish()
    }

    fun btnCancel(view: View) {
        setResult(RESULT_CANCELED)
        finish()
    }
}