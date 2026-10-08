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

public enum CarbUnit {
    GRAMS(
            R.string.carb_unit_grams_label,
            R.string.carb_unit_grams_short,
            1.0
    ),
    KE(
            R.string.carb_unit_ke_label,
            R.string.carb_unit_ke_short,
            10.0
    ),
    BE(
            R.string.carb_unit_be_label,
            R.string.carb_unit_be_short,
            12.0
    );

    @StringRes private final int labelResId;
    @StringRes private final int shortNameResId;
    private final double gramsFactor;

    CarbUnit(
            @StringRes int labelResId,
            @StringRes int shortNameResId,
            double gramsFactor
    ) {
        this.labelResId = labelResId;
        this.shortNameResId = shortNameResId;
        this.gramsFactor = gramsFactor;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public int getShortNameResId() {
        return shortNameResId;
    }

    public double getGramsFactor() {
        return gramsFactor;
    }

    public double toGrams(double value) {
        return value * gramsFactor;
    }

    public double fromGrams(double grams) {
        return gramsFactor > 0 ? grams / gramsFactor : 0.0;
    }

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
