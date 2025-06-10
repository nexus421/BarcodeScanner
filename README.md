# BarcodeScanner

[![](https://jitpack.io/v/nexus421/BarcodeScanner.svg)](https://jitpack.io/#nexus421/BarcodeScanner)

## Overview

A simple, lightweight Barcode/QR-Code Reader inside an AlertDialog. Built with Google ML-Kit and the new CameraX API.

## Features

- Fast barcode recognition
- Simple implementation
- Customizable UI
- Flashlight support
- Image capture functionality
- Continuous scanning mode

## Installation

### Gradle Setup

1. Add JitPack repository to your build file

```gradle
// build.gradle (top-level) or settings.gradle
repositories {
    // ...
    maven { url "https://jitpack.io" }
}
```

2. Add the dependency

```gradle
// build.gradle (app)
dependencies {
    implementation "com.github.nexus421:BarcodeScanner:2.2.0"
}
```

## Usage

### Basic Scanner

Create an instance of the BarcodeScannerDialog for one-time scanning:

```kotlin
BarcodeScannerDialogV2(this@Activity) { barcode ->
    Toast.makeText(this@Activity, barcode, Toast.LENGTH_SHORT).show()
}
```

### Continuous Scanner

For continuous scanning (multiple barcodes):

```kotlin
BarcodeScannerContinuousDialog(this@Activity) { barcode ->
    Toast.makeText(this@Activity, barcode, Toast.LENGTH_SHORT).show()
    // Return true to stop scanning, false to continue
    false
}
```

### Image Capture

Simple dialog to easily take pictures:

```kotlin
ImageCaptureDialog(this@Activity) { bitmap ->
    // Handle the captured image
}
```

## Customization

You can customize the scanner with various options:
- Custom buttons
- Flashlight control
- Camera settings

See the code documentation for more detailed information.

## License

[WTFPL](https://www.wtfpl.net/)

___

![example_barcodescanner](https://github.com/nexus421/BarcodeScanner/assets/24206344/6bf903e4-7383-45e0-bf70-0f4e49882eaf)
