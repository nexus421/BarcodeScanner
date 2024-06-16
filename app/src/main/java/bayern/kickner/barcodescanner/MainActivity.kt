package bayern.kickner.barcodescanner

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Surface
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bayern.kickner.barcode_scanner_library.BarcodeScannerDialogV2
import bayern.kickner.barcode_scanner_library.ImageCaptureDialog
import bayern.kickner.barcode_scanner_library.ImageCaptureResult
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn1).setOnClickListener {
            BarcodeScannerDialogV2(this) {
                runOnUiThread {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        findViewById<Button>(R.id.btn2).setOnClickListener {

        }

        findViewById<Button>(R.id.btn3).setOnClickListener {
            Configuration.ORIENTATION_PORTRAIT
            Configuration.ORIENTATION_LANDSCAPE
            Surface.ROTATION_0
            Surface.ROTATION_90
            Surface.ROTATION_180
            Surface.ROTATION_270

            val orientation = resources.configuration.orientation
            val rotation = windowManager.defaultDisplay.rotation
            println(orientation + rotation)
            ImageCaptureDialog(this, imageCaptureResult = ImageCaptureResult.File(File(filesDir, "fillleeee.jpg")) {
                val bitmap = BitmapFactory.decodeFile(it.absolutePath)
                findViewById<ImageView>(R.id.image).setImageBitmap(bitmap)
                true
            })
        }


    }
}