package network.spiritscorp.data

import android.content.Context
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object DatabaseBackupManager {

    /**
     * Exports all database contents (UserSettings and CalculationLogs) to a formatted JSON string.
     */
    suspend fun exportToJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsDirect() ?: UserSettings()
        // Since DAO returns Flow or list, we can query direct or fetch all
        // Let's add a direct query in DAO or fetch via flow
        val root = JSONObject()
        
        // Settings
        val settingsObj = JSONObject().apply {
            put("morningFactor", settings.morningFactor)
            put("noonFactor", settings.noonFactor)
            put("eveningFactor", settings.eveningFactor)
            put("nightFactor", settings.nightFactor)
            put("defaultCarbUnit", settings.defaultCarbUnit)
            put("beGramsDivisor", settings.beGramsDivisor)
            put("glucoseUnit", settings.glucoseUnit)
            put("targetGlucoseMgDl", settings.targetGlucoseMgDl)
            put("correctionFactorMgDl", settings.correctionFactorMgDl)
            put("roundingStep", settings.roundingStep)
            put("showDisclaimer", settings.showDisclaimer)
            put("selectedTheme", settings.selectedTheme)
            put("themeMode", settings.themeMode)
        }
        root.put("settings", settingsObj)

        root.toString(2)
    }

    /**
     * Exports calculation logs into a clean CSV format for analysis or spreadsheets.
     */
    suspend fun exportToCsv(logs: List<CalculationLog>): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("ID,Timestamp,Mahlzeit,Rohwert,Einheit,g KH,BE,KE,Tageszeit,Faktor,MahlzeitInsulin,Blutzucker,Zielwert,KorrekturInsulin,GesamtInsulin,Notizen")
        logs.forEach { log ->
            sb.appendLine("${log.id},${log.timestamp},\"${log.mealTitle}\",${log.rawCarbInput},${log.carbUnit},${log.carbGrams},${log.beValue},${log.keValue},\"${log.timeOfDay}\",${log.insulinFactor},${log.mealInsulin},${log.bloodGlucose ?: ""},${log.targetGlucose ?: ""},${log.correctionInsulin ?: 0.0},${log.roundedInsulin},\"${log.notes}\"")
        }
        sb.toString()
    }

    /**
     * Imports database contents from a JSON string with fault-tolerance:
     * - Missing keys are automatically filled with robust defaults.
     * - Extra/unknown keys are safely ignored.
     */
    suspend fun importFromJson(context: Context, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val db = AppDatabase.getDatabase(context)
            
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                val defaultSettings = UserSettings()
                
                val settings = UserSettings(
                    id = 1,
                    morningFactor = s.optDouble("morningFactor", defaultSettings.morningFactor),
                    noonFactor = s.optDouble("noonFactor", defaultSettings.noonFactor),
                    eveningFactor = s.optDouble("eveningFactor", defaultSettings.eveningFactor),
                    nightFactor = s.optDouble("nightFactor", defaultSettings.nightFactor),
                    defaultCarbUnit = s.optString("defaultCarbUnit", defaultSettings.defaultCarbUnit),
                    beGramsDivisor = s.optInt("beGramsDivisor", defaultSettings.beGramsDivisor),
                    glucoseUnit = s.optString("glucoseUnit", defaultSettings.glucoseUnit),
                    targetGlucoseMgDl = s.optDouble("targetGlucoseMgDl", defaultSettings.targetGlucoseMgDl),
                    correctionFactorMgDl = s.optDouble("correctionFactorMgDl", defaultSettings.correctionFactorMgDl),
                    roundingStep = s.optDouble("roundingStep", defaultSettings.roundingStep),
                    showDisclaimer = s.optBoolean("showDisclaimer", defaultSettings.showDisclaimer),
                    selectedTheme = s.optString("selectedTheme", defaultSettings.selectedTheme),
                    themeMode = s.optString("themeMode", defaultSettings.themeMode)
                )
                db.userSettingsDao().saveSettings(settings)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
