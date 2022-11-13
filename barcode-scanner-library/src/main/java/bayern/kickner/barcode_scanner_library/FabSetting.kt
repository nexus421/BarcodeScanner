package bayern.kickner.barcode_scanner_library

import android.app.Dialog
import android.graphics.drawable.Drawable

data class FabSetting(val fabIcon: Drawable, val onClick: (Dialog) -> Unit)
