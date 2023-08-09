package bayern.kickner.barcodescanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bayern.kickner.barcode_scanner_library.BarcodeScannerDialog
import bayern.kickner.barcode_scanner_library.ContinuousScanSettings
import bayern.kickner.barcode_scanner_library.Torch
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (false) {
            startActivity(Intent(this, MainActivity2::class.java))
            finish()
            return
        }

        BarcodeScannerDialog(
            this,
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
            continuousScanSettings = ContinuousScanSettings(1500), torch = Torch.Manual
        ) { customerID ->
            Toast.makeText(this, customerID, Toast.LENGTH_SHORT).show()
        }
    }
}