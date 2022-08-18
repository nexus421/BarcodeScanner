package bayern.kickner.barcodescanner

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bayern.kickner.barcode_scanner_library.BarcodeScannerDialog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BarcodeScannerDialog(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }
}