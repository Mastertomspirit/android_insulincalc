package network.spiritscorp.ui.components

/*
 * Copyright (C) 2026 Tom Spirit
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats a Double value into a localized display string with specified maximum decimal digits.
 * Grouping (thousands separator) is disabled by default for clean diabetic dosage and carb presentation.
 *
 * @param maxDecimals Maximum number of fraction digits to display (defaults to 2).
 * @param locale The locale to format the number for (e.g. Locale.GERMAN or Locale.US).
 * @return Formatted string representation of the number.
 */
fun Double.toDisplayString(maxDecimals: Int = 2, locale: Locale): String {
    val nf = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = maxDecimals
        minimumFractionDigits = 0
        isGroupingUsed = false
    }
    return nf.format(this)
}