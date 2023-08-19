package bayern.kickner.barcode_scanner_library

import android.widget.TextView
import androidx.cardview.widget.CardView

/**
 * @param title Text which will be displayed at the top
 * @param customLayoutSettings The layout is a [CardView] with a [TextView] inside. If null, defaults will be used. Otherwise you can
 * use this function to customize the CardView/TextView as you wish.
 */
data class TitleLayout(val title: String = "Barcode scannen", val customLayoutSettings: ((CardView, TextView) -> Unit)? = null)
