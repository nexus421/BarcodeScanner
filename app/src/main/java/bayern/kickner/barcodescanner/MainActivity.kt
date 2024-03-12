package bayern.kickner.barcodescanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import bayern.kickner.barcode_scanner_library.BarcodeScannerContinuousDialog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (false) {
            startActivity(Intent(this, MainActivity2::class.java))
            finish()
            return
        }

        findViewById<Button>(R.id.btn1).setOnClickListener {
            BarcodeScannerContinuousDialog(this) {
                runOnUiThread {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        findViewById<Button>(R.id.btn2).setOnClickListener {

        }

        findViewById<Button>(R.id.btn3).setOnClickListener {

        }


    }
}