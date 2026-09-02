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

public enum CarbUnit {
    GRAMS("Gramm Kohlenhydrate", "g KH", 1.0),
    KE("Kohlenhydrateinheit (10g)", "KE", 10.0),
    BE("Broteinheit (12g)", "BE", 12.0);

    private final String label;
    private final String shortName;
    private final double gramsFactor;

    CarbUnit(String label, String shortName, double gramsFactor) {
        this.label = label;
        this.shortName = shortName;
        this.gramsFactor = gramsFactor;
    }

    public String getLabel() {
        return label;
    }

    public String getShortName() {
        return shortName;
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
        String trimmed = str.trim().toUpperCase();
        if ("BE".equals(trimmed) || "BROTEINHEIT".equals(trimmed)) {
            return BE;
        } else if ("KE".equals(trimmed) || "KOHLENHYDRATEINHEIT".equals(trimmed) || "KHE".equals(trimmed)) {
            return KE;
        } else {
            return GRAMS;
        }
    }
}
