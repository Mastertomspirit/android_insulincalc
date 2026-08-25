package network.spiritscorp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_logs")
data class CalculationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mealTitle: String = "Mahlzeit",
    val rawCarbInput: Double = 0.0,
    val carbUnit: String = "g KH",
    val carbGrams: Double = 0.0,
    val beValue: Double = 0.0,
    val keValue: Double = 0.0,
    val timeOfDay: String = "Morgens",
    val insulinFactor: Double = 1.0, // IE pro KE (10g)
    val mealInsulin: Double = 0.0,
    val bloodGlucose: Double? = null,
    val targetGlucose: Double? = null,
    val correctionFactor: Double? = null,
    val correctionInsulin: Double? = null,
    val totalInsulin: Double = 0.0,
    val roundedInsulin: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    val morningFactor: Double = 1.50,
    val noonFactor: Double = 1.00,
    val eveningFactor: Double = 1.20,
    val nightFactor: Double = 0.80,
    val defaultCarbUnit: String = "GRAMS",
    val beGramsDivisor: Int = 12, // 12g or 10g per BE
    val glucoseUnit: String = "mg/dl",
    val targetGlucoseMgDl: Double = 120.0,
    val correctionFactorMgDl: Double = 50.0, // 1 IE senkt BZ um 40 mg/dl
    val roundingStep: Double = 0.5, // 0.1, 0.5, or 1.0
    val showDisclaimer: Boolean = true,
    val selectedTheme: String = "MEDICAL_TEAL",
    val themeMode: String = "SYSTEM" // "SYSTEM", "LIGHT", "DARK"
)

data class CalculationSummary(
    val carbGrams: Double,
    val keValue: Double,
    val beValue: Double,
    val factorUsed: Double,
    val mealInsulin: Double,
    val bloodGlucoseInput: Double?,
    val targetGlucose: Double?,
    val correctionInsulin: Double,
    val rawTotalInsulin: Double,
    val roundedTotalInsulin: Double,
    val roundingStep: Double,
    val isHypoRisk: Boolean,
    val advisoryNote: String
)
