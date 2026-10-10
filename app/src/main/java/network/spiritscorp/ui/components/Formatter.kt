package network.spiritscorp.ui.components

import java.text.NumberFormat
import java.util.Locale

fun Double.toDisplayString(maxDecimals: Int = 2, locale: Locale): String {
    val nf = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = maxDecimals
        minimumFractionDigits = 0
        isGroupingUsed = false
    }
    return nf.format(this)
}