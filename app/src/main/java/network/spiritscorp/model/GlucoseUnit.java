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

public enum GlucoseUnit {
    MG_DL("Milligramm pro Deziliter", "mg/dl"),
    MMOL_L("Millimol pro Liter", "mmol/l");

    private final String label;
    private final String shortName;

    GlucoseUnit(String label, String shortName) {
        this.label = label;
        this.shortName = shortName;
    }

    public String getLabel() {
        return label;
    }

    public String getShortName() {
        return shortName;
    }

    public double toMgDl(double value) {
        return this == MMOL_L ? value * 18.0182 : value;
    }

    public double fromMgDl(double mgDl) {
        return this == MMOL_L ? mgDl / 18.0182 : mgDl;
    }

    public static GlucoseUnit fromString(String str) {
        if (str == null) return MG_DL;
        String normalized = str.trim().toLowerCase();
        if (normalized.equals("mmol/l") || normalized.equals("mmol") || normalized.equals("mmol_l")) {
            return MMOL_L;
        }
        return MG_DL;
    }
}
