package bayern.kickner.barcodescanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import bayern.kickner.barcode_scanner_library.BarcodeScannerDialog
import bayern.kickner.barcode_scanner_library.ButtonSettings
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (true) {
            startActivity(Intent(this, MainActivity2::class.java))
            finish()
            return
        }

        BarcodeScannerDialog(
            this,
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
            buttonSettings = ButtonSettings(AppCompatResources.getDrawable(this, bayern.kickner.barcode_scanner_library.R.drawable.ic_baseline_cancel_24)!!) {
                Toast.makeText(this, "Clicked FAB", Toast.LENGTH_SHORT).show()
            }) { customerID ->
            Toast.makeText(this, customerID, Toast.LENGTH_SHORT).show()
        }
    }
}