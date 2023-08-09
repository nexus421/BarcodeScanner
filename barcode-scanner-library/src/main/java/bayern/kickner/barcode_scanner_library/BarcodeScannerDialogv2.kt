package bayern.kickner.barcode_scanner_library

import android.Manifest
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.google.mlkit.vision.barcode.common.Barcode


private data class BarcodeScannerDialogV2(
    private val activity: ComponentActivity,
    private val barcodeFormats: List<Int> = listOf(Barcode.FORMAT_ALL_FORMATS),
    private val title: String? = "Barcode scannen",
    private val torch: Torch = Torch.Manual,
    private val onResult: (String) -> Unit
) {

    private val dialog: AlertDialog.Builder
    private val rootLayout: ConstraintLayout = View.inflate(activity, R.layout.camera_dialog_layout, null) as ConstraintLayout

    init {
        dialog = if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
            Log.e("BarcodeScanner", "Camera permission is missing.")
            AlertDialog.Builder(activity).setTitle("Permission").setMessage("Camera permission is missing!")
        } else {
            AlertDialog.Builder(activity).setView(rootLayout).setCancelable(true)
        }
    }

}