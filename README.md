# BarcodeScanner

This is a simple Barcode/QR-Code Reader inside an AlertDialog. Based on the Google ML-Kit with the new CameraX. Based on the Android Playground.

Create an instance of the BarcodeScannerDialog and receive fast barcode recognition.

    BarcodeScannerDialogV2(this@Activity) { barcode ->
            Toast.makeText(this@Activity, barcode, Toast.LENGTH_SHORT).show()
        }

    BarcodeScannerContinuousDialog(this@Activity) { barcode ->
        Toast.makeText(this@Activity, barcode, Toast.LENGTH_SHORT).show()
        false
    }

Small, Simple and easy to use. You can set up a custom button and also use the phones flashlight.
See the code documentation for more information.

To add this library:

build.gradle (top-level) or settings.gradle:
repositories {
...
maven("https://jitpack.io")
}

build.gradle app:
implementation("com.github.nexus421:BarcodeScanner:2.1.0")

Example:

![example_barcodescanner](https://github.com/nexus421/BarcodeScanner/assets/24206344/6bf903e4-7383-45e0-bf70-0f4e49882eaf)
