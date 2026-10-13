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
 * Enum representing carbohydrate units used in diabetes management calculations.
 * Supports grams (g), Kohlenhydrateinheiten (KE, 10g KH), and Broteinheiten (BE, configurable grams per BE).
 */
public enum CarbUnit {
    /** Pure grams of carbohydrates (1g = 1g). */
    GRAMS(
            R.string.carb_unit_grams_label,
            R.string.carb_unit_grams_short,
            1.0
    ),
    /** Kohlenhydrateinheit (1 KE = 10g KH). */
    KE(
            R.string.carb_unit_ke_label,
            R.string.carb_unit_ke_short,
            10.0
    ),
    /** Broteinheit (1 BE = typically 12g KH). */
    BE(
            R.string.carb_unit_be_label,
            R.string.carb_unit_be_short,
            12.0
    );

    @StringRes private final int labelResId;
    @StringRes private final int shortNameResId;
    private final double gramsFactor;

    /**
     * Constructs a CarbUnit enum constant.
     *
     * @param labelResId String resource ID for full unit name.
     * @param shortNameResId String resource ID for short unit symbol.
     * @param gramsFactor Multiplier to convert 1 unit into grams of carbohydrates.
     */
    CarbUnit(
            @StringRes int labelResId,
            @StringRes int shortNameResId,
            double gramsFactor
    ) {
        this.labelResId = labelResId;
        this.shortNameResId = shortNameResId;
        this.gramsFactor = gramsFactor;
    }

    /** Returns the string resource ID for the full label of this carb unit. */
    public int getLabelResId() {
        return labelResId;
    }

    /** Returns the string resource ID for the short abbreviation of this carb unit. */
    public int getShortNameResId() {
        return shortNameResId;
    }

    /** Returns the conversion factor to translate this unit into grams. */
    public double getGramsFactor() {
        return gramsFactor;
    }

    /** Converts a given amount in this unit to grams of carbohydrates. */
    public double toGrams(double value) {
        return value * gramsFactor;
    }

    /** Converts a given amount in grams into this carbohydrate unit. */
    public double fromGrams(double grams) {
        return gramsFactor > 0 ? grams / gramsFactor : 0.0;
    }

    /** Parses a string representation into a corresponding CarbUnit, defaulting to GRAMS. */
    public static CarbUnit fromString(String str) {
        if (str == null) return GRAMS;
        String trimmed = str.trim().toUpperCase(Locale.getDefault());
        if ("BE".equals(trimmed) || "BROTEINHEIT".equals(trimmed)) {
            return BE;
        } else if ("KE".equals(trimmed) || "KOHLENHYDRATEINHEIT".equals(trimmed) || "KHE".equals(trimmed)) {
            return KE;
        } else {
            return GRAMS;
        }
    }
}
