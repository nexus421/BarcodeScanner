# BarcodeScanner

This is a simple Barcode/QR-Code Reader inside an AlertDialog. Based on the Google ML-Kit with the new CameraX. Based on the Android Playground.

Create an instance of the BarcodeScannerDialog and receive fast barcode recognition.

BarcodeScannerDialog(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

Small, Simple and easy to use.

To add this library:

build.gradle (top-level) or settings.gradle:
repositories {
	...
	maven { url 'https://jitpack.io' }
}
                        
build.gradle app:
implementation 'com.github.nexus421:BarcodeScanner:1.0.1'


![Screenshot_20220818_203455](https://user-images.githubusercontent.com/24206344/185469083-daf0ee08-7f3a-4119-8a2d-afb80c396c36.png)
