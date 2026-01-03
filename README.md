# BarcodeScanner

## Overview

A simple and lightweight Barcode/QR-Code Reader presented inside an `AlertDialog`. It's built with
Google's ML Kit and the CameraX API.

## Features

- Fast barcode recognition
- Simple implementation
- Customizable UI
- Flashlight support
- Image capture functionality
- Continuous scanning mode

## Installation

1. **Add the repository**

   Add the Maven repository to your `settings.gradle.kts`:

   ```kotlin
   // settings.gradle.kts
   dependencyResolutionManagement {
       repositories {
           // ...
           maven {
               name = "nexus421Maven"
               url = uri("https://maven.kickner.bayern/releases")
           }
       }
   }
   ```

2. **Add the dependency**

   Add the library dependency to your app's `build.gradle.kts`:

   ```kotlin
   // app/build.gradle.kts
   dependencies {
       implementation("bayern.kickner:BarcodeScanner:2.2.3")
   }
   ```

## Usage

### Basic Scanner

Create an instance of `BarcodeScannerDialogV2` for one-time scanning:

```kotlin
BarcodeScannerDialogV2(this@Activity) { barcode ->
    Toast.makeText(this@Activity, barcode, Toast.LENGTH_SHORT).show()
}
```

### Continuous Scanner

For continuous scanning (multiple barcodes), use `BarcodeScannerContinuousDialog`:

```kotlin
BarcodeScannerContinuousDialog(this@Activity) { barcode ->
    Toast.makeText(this@Activity, barcode, Toast.LENGTH_SHORT).show()
    // Return true to stop scanning, or false to continue
    false
}
```

### Image Capture

The `ImageCaptureDialog` provides a simple way to take pictures:

```kotlin
ImageCaptureDialog(this@Activity) { bitmap ->
    // Handle the captured bitmap
}
```

## Customization

The dialogs can be customized with various options:

- Custom buttons
- Flashlight control
- Camera settings

Please refer to the source code documentation for more details.

## License

[WTFPL](https://www.wtfpl.net/)

---

![Example of the barcode scanner in action](https://github.com/nexus421/BarcodeScanner/assets/24206344/6bf903e4-7383-45e0-bf70-0f4e49882eaf)
