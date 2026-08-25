package network.spiritscorp.model

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

enum class CarbUnit(val label: String, val shortName: String, val gramsFactor: Double) {
    GRAMS("Gramm Kohlenhydrate", "g KH", 1.0),
    KE("Kohlenhydrateinheit (10g)", "KE", 10.0),
    BE("Broteinheit (12g)", "BE", 12.0);

    fun toGrams(value: Double): Double = value * gramsFactor
    fun fromGrams(grams: Double): Double = if (gramsFactor > 0) grams / gramsFactor else 0.0
}

enum class GlucoseUnit(val label: String, val shortName: String) {
    MG_DL("Milligramm pro Deziliter", "mg/dl"),
    MMOL_L("Millimol pro Liter", "mmol/l");

    fun toMgDl(value: Double): Double = when (this) {
        MG_DL -> value
        MMOL_L -> value * 18.0182
    }

    fun fromMgDl(mgDl: Double): Double = when (this) {
        MG_DL -> mgDl
        MMOL_L -> mgDl / 18.0182
    }

    companion object {
        fun fromString(str: String?): GlucoseUnit {
            return when (str?.trim()?.lowercase()) {
                "mmol/l", "mmol", "mmol_l" -> MMOL_L
                else -> MG_DL
            }
        }
    }
}
