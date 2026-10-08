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

public enum GlucoseUnit {
    MG_DL(
            R.string.glucose_unit_mg_dl_label,
            R.string.glucose_unit_mg_dl_short
    ),
    MMOL_L(
            R.string.glucose_unit_mmol_l_label,
            R.string.glucose_unit_mmol_l_short
    );

    @StringRes private final int labelResId;
    @StringRes private final int shortNameResId;

    GlucoseUnit(
            @StringRes int labelResId,
            @StringRes int shortNameResId
    ) {
        this.labelResId = labelResId;
        this.shortNameResId = shortNameResId;
    }

    public int getLabelResId() {
        return labelResId;
    }

    public int getShortNameResId() {
        return shortNameResId;
    }

    public double toMgDl(double value) {
        return this == MMOL_L ? value * 18.0182 : value;
    }

    public double fromMgDl(double mgDl) {
        return this == MMOL_L ? mgDl / 18.0182 : mgDl;
    }

    public static GlucoseUnit fromString(String str) {
        if (str == null) return MG_DL;
        String normalized = str.trim().toLowerCase(Locale.getDefault());
        if (normalized.equals("mmol/l") || normalized.equals("mmol") || normalized.equals("mmol_l")) {
            return MMOL_L;
        }
        return MG_DL;
    }
}
