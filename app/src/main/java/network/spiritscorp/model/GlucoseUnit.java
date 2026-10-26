package network.spiritscorp.model;

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

import androidx.annotation.StringRes;

import java.util.Locale;

import network.spiritscorp.R;

/**
 * Enum representing blood glucose measurement units (mg/dl or mmol/l).
 * <p>
 * Provides factor conversions between milligrams per deciliter (mg/dl) and millimoles per liter (mmol/l),
 * as well as string resource identifiers for full titles and short unit abbreviations.
 */
public enum GlucoseUnit {
    /** Milligrams per deciliter (mg/dl), standard clinical unit used primarily in Germany, USA, and other regions. */
    MG_DL(
            R.string.glucose_unit_mg_dl_label,
            R.string.glucose_unit_mg_dl_short
    ),
    /** Millimoles per liter (mmol/l), standard SI unit used primarily in UK, Canada, and clinical international practice. */
    MMOL_L(
            R.string.glucose_unit_mmol_l_label,
            R.string.glucose_unit_mmol_l_short
    );

    @StringRes private final int labelResId;
    @StringRes private final int shortNameResId;

    /**
     * Constructs a GlucoseUnit enum constant with localized label and abbreviation resources.
     *
     * @param labelResId     String resource ID for the full descriptive label.
     * @param shortNameResId String resource ID for the compact abbreviation symbol.
     */
    GlucoseUnit(
            @StringRes int labelResId,
            @StringRes int shortNameResId
    ) {
        this.labelResId = labelResId;
        this.shortNameResId = shortNameResId;
    }

    /**
     * Returns the string resource ID for the full label of this glucose unit.
     *
     * @return The label resource ID.
     */
    public int getLabelResId() {
        return labelResId;
    }

    /**
     * Returns the string resource ID for the short abbreviation of this glucose unit.
     *
     * @return The short name resource ID.
     */
    public int getShortNameResId() {
        return shortNameResId;
    }

    /**
     * Converts a blood glucose value from this unit to milligrams per deciliter (mg/dl).
     * <p>
     * If the unit is {@link #MMOL_L}, multiplies by the molecular conversion factor ~18.0182.
     * If the unit is already {@link #MG_DL}, returns the value unchanged.
     *
     * @value The blood glucose value in current unit.
     * @return The equivalent value in mg/dl.
     */
    public double toMgDl(double value) {
        return this == MMOL_L ? value * 18.0182 : value;
    }

    /**
     * Converts a blood glucose value given in mg/dl to this unit.
     * <p>
     * If the unit is {@link #MMOL_L}, divides by the molecular conversion factor ~18.0182.
     * If the unit is {@link #MG_DL}, returns the mg/dl value unchanged.
     *
     * @param mgDl The blood glucose value in mg/dl.
     * @return The equivalent value in this unit.
     */
    public double fromMgDl(double mgDl) {
        return this == MMOL_L ? mgDl / 18.0182 : mgDl;
    }

    /**
     * Parses a string representation into a corresponding {@link GlucoseUnit}.
     * <p>
     * Recognizes various standard strings for mmol/l (case-insensitive); defaults to {@link #MG_DL} for null or unrecognized inputs.
     *
     * @param str The raw input string.
     * @return The matching {@link GlucoseUnit}.
     */
    public static GlucoseUnit fromString(String str) {
        if (str == null) return MG_DL;
        String normalized = str.trim().toLowerCase(Locale.getDefault());
        if (normalized.equals("mmol/l") || normalized.equals("mmol") || normalized.equals("mmol_l")) {
            return MMOL_L;
        }
        return MG_DL;
    }
}
