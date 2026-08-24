package com.example.model

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
}
