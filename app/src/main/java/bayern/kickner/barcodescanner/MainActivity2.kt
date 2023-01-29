package bayern.kickner.barcodescanner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import bayern.kickner.barcode_scanner_library.BarcodeScannerDialog
import bayern.kickner.barcode_scanner_library.ButtonSettings
import bayern.kickner.barcode_scanner_library.ContinuousScanSettings
import bayern.kickner.barcodescanner.ui.theme.BarcodeScannerTheme

class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BarcodeScannerDialog(this, buttonSettings = ButtonSettings(getDrawable(R.drawable.ic_launcher_foreground)!!) {}, continuousScanSettings = ContinuousScanSettings(1500, true)) {
            Log.d("TestTest", it)
        }
        setContent {
            BarcodeScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BarcodeScannerTheme {
        Greeting("Android")
    }
}