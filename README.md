# BarcodeScanner

## Overview

A simple and lightweight Barcode/QR-Code Reader presented inside an `AlertDialog`. It's built with
Google's ML Kit and the CameraX API.

## Features

- Fast barcode recognition
- Simple implementation - just construct a dialog, no `show()` call needed
- Rich per-barcode metadata (format, value type, bounding box, corner points) via ML Kit's own `Barcode` type
- Configurable dialog size (small / medium / large / fullscreen)
- Customizable UI (title, extra button, torch)
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
       implementation("bayern.kickner:BarcodeScanner:3.0.0")
   }
   ```

## Which dialog should I use?

| Class                                      | Use when                                                                                         | Result                                                                           |
|--------------------------------------------|--------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| **`BarcodeScannerDialogV3`** (recommended) | One-time scan, and you want every barcode ML Kit found in the accepted frame, with full metadata | `List<Barcode>`                                                                  |
| `BarcodeScannerContinuousDialog`           | You want to keep scanning - e.g. multiple items in a row - until you decide to stop              | one `String` per scan, dialog stays open until you return `true`                 |
| `BarcodeScannerDialogV2` (deprecated)      | Legacy code only                                                                                 | a single `String`                                                                |
| `ImageCaptureDialog<T>`                    | You want to take a photo, not decode a barcode                                                   | `ByteArray`, `File`, or `Uri`, depending on the `ImageCaptureResult` you pass in |

## Usage

### Basic Scanner (recommended)

Create an instance of `BarcodeScannerDialogV3` for one-time scanning. `onResult` receives every barcode ML Kit
detected in the accepted frame, in ML Kit's own detection order - ML Kit doesn't expose a confidence score per
barcode, so this is *not* re-sorted by "best match":

```kotlin
BarcodeScannerDialogV3(this@Activity) { barcodes ->
    val value = barcodes.first().rawValue ?: return@BarcodeScannerDialogV3
    Toast.makeText(this@Activity, value, Toast.LENGTH_SHORT).show()
}
```

Each entry is ML Kit's own `Barcode` type, so you also get `format`, `valueType`, `boundingBox` and
`cornerPoints` for free - handy if you want to draw a marker on the detected barcode, for example.

You can control how large the dialog window is via `dialogSize`:

```kotlin
BarcodeScannerDialogV3(this@Activity, dialogSize = DialogSize.Medium) { barcodes ->
    // ...
}
```

`DialogSize` offers `Small`, `Medium`, `Large` and `Fullscreen` (default). Regardless of the size chosen, the
entire visible camera preview is what gets scanned - there is no separate, smaller "scan window" inside it.

> `BarcodeScannerDialogV2` is deprecated in favor of `BarcodeScannerDialogV3` - it only returns a single decoded
> `String` per scan (instead of every detected barcode with its metadata) and is kept solely for backward
> compatibility. New integrations should use `BarcodeScannerDialogV3`.

### Continuous Scanner

For continuous scanning (multiple barcodes, one after another, in the same session), use
`BarcodeScannerContinuousDialog`:

```kotlin
BarcodeScannerContinuousDialog(this@Activity) { barcode ->
    Toast.makeText(this@Activity, barcode, Toast.LENGTH_SHORT).show()
    // Return true to stop scanning, or false to continue
    false
}
```

### Image Capture

`ImageCaptureDialog<T>` provides a simple way to take pictures. It's generic over how you want to receive the
image - pass an `ImageCaptureResult.ByteArray`, `.File`, or `.Uri`:

```kotlin
ImageCaptureDialog(
    this@Activity,
    imageCaptureResult = ImageCaptureResult.File(File(filesDir, "capture.jpg")) { file ->
        // Handle the saved file, e.g. load it into an ImageView
        true // return true to dismiss the dialog, false to keep it open for another shot
    }
)
```

> Known issue: the captured image is currently always rotated by 90°.

## Customization

Every dialog is configured through named, defaulted constructor parameters instead of a builder chain:

- **`TitleLayout`** - optional header text, or a callback to fully customize the header `CardView`/`TextView`
- **`ButtonSettings`** *(scanner dialogs only)* - an optional extra button (icon + click handler) shown bottom-left
- **`Torch`** *(scanner dialogs only)* - `ForceOn`, `Off`, or `Manual` (default; shows a torch toggle button)
- **`DialogSize`** *(`BarcodeScannerDialogV3` only)* - `Small`, `Medium`, `Large`, or `Fullscreen`
- **`Flashlight`, `CaptureMode`, `CameraRotation`** *(`ImageCaptureDialog` only)* - flash mode, latency vs.
  quality trade-off, and forced capture rotation

Every dialog exposes `onError` (defaults to logging); the scanner dialogs additionally take an optional
`onDismiss` callback. Refer to each class's KDoc for the full list of parameters.

## License

[WTFPL](https://www.wtfpl.net/)

---

![Example of the barcode scanner in action](https://github.com/nexus421/BarcodeScanner/assets/24206344/6bf903e4-7383-45e0-bf70-0f4e49882eaf)
