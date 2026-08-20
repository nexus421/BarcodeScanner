package bayern.kickner.barcode_scanner_library

/**
 * Controls the size of the dialog window that hosts the camera preview in [BarcodeScannerDialogV3].
 * The camera preview always fills the entire dialog window, no matter which size is chosen - the whole visible
 * preview area is what gets scanned for barcodes, there is no separate, smaller "scan window" inside it.
 *
 * @param widthFraction fraction (0f..1f) of the screen width the dialog window should occupy. Ignored for [Fullscreen].
 * @param heightFraction fraction (0f..1f) of the screen height the dialog window should occupy. Ignored for [Fullscreen].
 */
enum class DialogSize(val widthFraction: Float, val heightFraction: Float) {
    Small(0.6f, 0.4f),
    Medium(0.8f, 0.55f),
    Large(0.95f, 0.75f),

    /**
     * Uses the entire screen, without the dialog's usual rounded background/margins around it.
     */
    Fullscreen(1f, 1f)
}
