package bayern.kickner.barcode_scanner_library

import android.graphics.drawable.Drawable

/**
 * Used for an additional button inside the [BarcodeScannerDialogV2] and [BarcodeScannerDialog] for a custom action.
 * Located on the bottom left.
 *
 * @param btnIcon will be displayed inside the button
 * @param onClick will be called on this button click
 */
data class ButtonSettings(val btnIcon: Drawable, val onClick: () -> Unit)
