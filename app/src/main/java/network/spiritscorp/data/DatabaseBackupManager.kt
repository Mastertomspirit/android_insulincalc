package network.spiritscorp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result model for import operations providing granular feedback.
 */
data class ImportResult(
    val success: Boolean,
    val importedLogsCount: Int = 0,
    val importedSettings: Boolean = false,
    val message: String = ""
)

/**
 * Handles complete JSON and CSV export/import for user settings and calculation logs.
 * Designed with fault tolerance to ensure the application never crashes on corrupted files.
 */
object DatabaseBackupManager {

    private const val BACKUP_VERSION = 1
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY)

    /**
     * Exports all database contents (UserSettings and all CalculationLogs) to a formatted JSON string.
     */
    suspend fun exportToJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsDirect() ?: UserSettings()
        val logs = db.calculationLogDao().getAllLogsDirect()
        exportToJson(settings, logs)
    }

    /**
     * Pure function to generate a JSON backup string from settings and a list of logs.
     */
    fun exportToJson(settings: UserSettings, logs: List<CalculationLog>): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("app", "InsulinCalculator")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportedDate", isoDateFormat.format(Date()))

        // Serialize Settings
        val settingsObj = JSONObject().apply {
            put("id", settings.id)
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

        // Serialize ALL Calculation Logs
        val logsArray = JSONArray()
        logs.forEach { log ->
            val logObj = JSONObject().apply {
                put("id", log.id)
                put("timestamp", log.timestamp)
                put("mealTitle", log.mealTitle)
                put("rawCarbInput", log.rawCarbInput)
                put("carbUnit", log.carbUnit)
                put("carbGrams", log.carbGrams)
                put("beValue", log.beValue)
                put("keValue", log.keValue)
                put("timeOfDay", log.timeOfDay)
                put("insulinFactor", log.insulinFactor)
                put("mealInsulin", log.mealInsulin)
                if (log.bloodGlucose != null) put("bloodGlucose", log.bloodGlucose)
                if (log.targetGlucose != null) put("targetGlucose", log.targetGlucose)
                if (log.correctionFactor != null) put("correctionFactor", log.correctionFactor)
                if (log.correctionInsulin != null) put("correctionInsulin", log.correctionInsulin)
                put("totalInsulin", log.totalInsulin)
                put("roundedInsulin", log.roundedInsulin)
                put("notes", log.notes)
            }
            logsArray.put(logObj)
        }
        root.put("logs", logsArray)

        return root.toString(2)
    }

    /**
     * Exports ALL calculation logs into standard CSV format.
     */
    fun exportToCsv(logs: List<CalculationLog>): String {
        val sb = StringBuilder()
        // Standardized Header Row
        sb.appendLine("ID,Timestamp,Date,MealTitle,RawCarbInput,CarbUnit,CarbGrams,BE,KE,TimeOfDay,InsulinFactor,MealInsulin,BloodGlucose,TargetGlucose,CorrectionFactor,CorrectionInsulin,TotalInsulin,RoundedInsulin,Notes")

        logs.forEach { log ->
            val dateStr = isoDateFormat.format(Date(log.timestamp))
            val line = listOf(
                log.id.toString(),
                log.timestamp.toString(),
                escapeCsv(dateStr),
                escapeCsv(log.mealTitle),
                log.rawCarbInput.toString(),
                escapeCsv(log.carbUnit),
                log.carbGrams.toString(),
                log.beValue.toString(),
                log.keValue.toString(),
                escapeCsv(log.timeOfDay),
                log.insulinFactor.toString(),
                log.mealInsulin.toString(),
                log.bloodGlucose?.toString() ?: "",
                log.targetGlucose?.toString() ?: "",
                log.correctionFactor?.toString() ?: "",
                log.correctionInsulin?.toString() ?: "",
                log.totalInsulin.toString(),
                log.roundedInsulin.toString(),
                escapeCsv(log.notes)
            ).joinToString(",")
            sb.appendLine(line)
        }
        return sb.toString()
    }

    /**
     * Imports database contents from a JSON string into the database.
     * Fault-tolerant: will not crash on malformed keys or corrupted JSON.
     */
    suspend fun importFromJson(context: Context, jsonString: String): ImportResult = withContext(Dispatchers.IO) {
        val parsed = parseJson(jsonString)
        if (parsed == null) {
            return@withContext ImportResult(
                success = false,
                message = "The JSON data is invalid or could not be parsed."
            )
        }

        val (settings, logs) = parsed
        val db = AppDatabase.getDatabase(context)
        var importedSettings = false
        var importedLogsCount = 0

        try {
            if (settings != null) {
                db.userSettingsDao().saveSettings(settings)
                importedSettings = true
            }

            if (logs.isNotEmpty()) {
                db.calculationLogDao().insertLogs(logs)
                importedLogsCount = logs.size
            }

            if (!importedSettings && importedLogsCount == 0) {
                ImportResult(
                    success = false,
                    message = "No valid settings or log entries found in the JSON file."
                )
            } else {
                ImportResult(
                    success = true,
                    importedLogsCount = importedLogsCount,
                    importedSettings = importedSettings,
                    message = "Import successful: $importedLogsCount log entries${if (importedSettings) ", settings updated" else ""}."
                )
            }
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Failed to save imported data into database: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Pure function to safely parse a JSON string into UserSettings and CalculationLogs.
     * Returns null if the JSON string is completely unparseable or blank.
     */
    fun parseJson(jsonString: String): Pair<UserSettings?, List<CalculationLog>>? {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return null

        try {
            var parsedSettings: UserSettings? = null
            val parsedLogs = mutableListOf<CalculationLog>()

            if (trimmed.startsWith("[")) {
                // Direct array of CalculationLog objects
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseCalculationLogFromJson(obj)?.let { parsedLogs.add(it) }
                }
            } else {
                val root = JSONObject(trimmed)

                // Parse Settings if present
                if (root.has("settings")) {
                    val s = root.optJSONObject("settings")
                    if (s != null) {
                        val defaultSettings = UserSettings()
                        parsedSettings = UserSettings(
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
                    }
                }

                // Parse Logs if present
                if (root.has("logs")) {
                    val logsArray = root.optJSONArray("logs")
                    if (logsArray != null) {
                        for (i in 0 until logsArray.length()) {
                            val logObj = logsArray.optJSONObject(i) ?: continue
                            parseCalculationLogFromJson(logObj)?.let { parsedLogs.add(it) }
                        }
                    }
                }
            }

            return Pair(parsedSettings, parsedLogs)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseCalculationLogFromJson(obj: JSONObject): CalculationLog? {
        try {
            val mealTitle = obj.optString("mealTitle", "Mahlzeit")
            val carbGrams = obj.optDouble("carbGrams", 0.0)
            val rawCarbInput = obj.optDouble("rawCarbInput", carbGrams)
            val carbUnit = obj.optString("carbUnit", "g KH")
            val beValue = obj.optDouble("beValue", carbGrams / 12.0)
            val keValue = obj.optDouble("keValue", carbGrams / 10.0)
            val timeOfDay = obj.optString("timeOfDay", "Mahlzeit")
            val insulinFactor = obj.optDouble("insulinFactor", 1.0)
            val mealInsulin = obj.optDouble("mealInsulin", 0.0)

            val bloodGlucose = if (obj.has("bloodGlucose") && !obj.isNull("bloodGlucose")) obj.optDouble("bloodGlucose") else null
            val targetGlucose = if (obj.has("targetGlucose") && !obj.isNull("targetGlucose")) obj.optDouble("targetGlucose") else null
            val correctionFactor = if (obj.has("correctionFactor") && !obj.isNull("correctionFactor")) obj.optDouble("correctionFactor") else null
            val correctionInsulin = if (obj.has("correctionInsulin") && !obj.isNull("correctionInsulin")) obj.optDouble("correctionInsulin") else null

            val totalInsulin = obj.optDouble("totalInsulin", mealInsulin)
            val roundedInsulin = obj.optDouble("roundedInsulin", totalInsulin)
            val notes = obj.optString("notes", "")
            val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            val id = obj.optLong("id", 0L)

            return CalculationLog(
                id = id,
                timestamp = timestamp,
                mealTitle = mealTitle,
                rawCarbInput = rawCarbInput,
                carbUnit = carbUnit,
                carbGrams = carbGrams,
                beValue = beValue,
                keValue = keValue,
                timeOfDay = timeOfDay,
                insulinFactor = insulinFactor,
                mealInsulin = mealInsulin,
                bloodGlucose = bloodGlucose,
                targetGlucose = targetGlucose,
                correctionFactor = correctionFactor,
                correctionInsulin = correctionInsulin,
                totalInsulin = totalInsulin,
                roundedInsulin = roundedInsulin,
                notes = notes
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Imports logs from a CSV formatted string into the database.
     */
    suspend fun importFromCsv(context: Context, csvString: String): ImportResult = withContext(Dispatchers.IO) {
        val logs = parseCsv(csvString)
        if (logs.isEmpty()) {
            return@withContext ImportResult(
                success = false,
                message = "No valid log entries found in the CSV data."
            )
        }

        try {
            val db = AppDatabase.getDatabase(context)
            db.calculationLogDao().insertLogs(logs)
            ImportResult(
                success = true,
                importedLogsCount = logs.size,
                message = "Successfully imported ${logs.size} log entries from CSV."
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Failed to save CSV logs to database: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Pure function to safely parse CSV text into a list of CalculationLogs.
     */
    fun parseCsv(csvString: String): List<CalculationLog> {
        val trimmed = csvString.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val parsedLogs = mutableListOf<CalculationLog>()

        // Check if line 0 is a header row
        val firstLine = lines.first().lowercase()
        val startIndex = if (firstLine.contains("mahlzeit") || firstLine.contains("meal") || firstLine.contains("timestamp") || firstLine.contains("id")) 1 else 0

        for (i in startIndex until lines.size) {
            val rawLine = lines[i]
            val tokens = splitCsvLine(rawLine)
            if (tokens.isEmpty()) continue

            try {
                // Try parsing standard layout:
                // ID(0), Timestamp(1), Date(2), MealTitle(3), RawCarbInput(4), CarbUnit(5), CarbGrams(6), BE(7), KE(8), TimeOfDay(9), InsulinFactor(10), MealInsulin(11), BloodGlucose(12), TargetGlucose(13), CorrectionFactor(14), CorrectionInsulin(15), TotalInsulin(16), RoundedInsulin(17), Notes(18)
                // Or legacy layout:
                // ID(0), Timestamp(1), Mahlzeit(2), Rohwert(3), Einheit(4), g KH(5), BE(6), KE(7), Tageszeit(8), Faktor(9), MahlzeitInsulin(10), Blutzucker(11), Zielwert(12), KorrekturInsulin(13), GesamtInsulin(14), Notizen(15)

                val log: CalculationLog? = if (tokens.size >= 18) {
                    // Full format
                    val id = tokens.getOrNull(0)?.toLongOrNull() ?: 0L
                    val timestamp = tokens.getOrNull(1)?.toLongOrNull() ?: System.currentTimeMillis()
                    val mealTitle = tokens.getOrNull(3)?.ifBlank { "Mahlzeit" } ?: "Mahlzeit"
                    val rawCarbInput = tokens.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                    val carbUnit = tokens.getOrNull(5) ?: "g KH"
                    val carbGrams = tokens.getOrNull(6)?.toDoubleOrNull() ?: rawCarbInput
                    val beValue = tokens.getOrNull(7)?.toDoubleOrNull() ?: (carbGrams / 12.0)
                    val keValue = tokens.getOrNull(8)?.toDoubleOrNull() ?: (carbGrams / 10.0)
                    val timeOfDay = tokens.getOrNull(9)?.ifBlank { "Mahlzeit" } ?: "Mahlzeit"
                    val insulinFactor = tokens.getOrNull(10)?.toDoubleOrNull() ?: 1.0
                    val mealInsulin = tokens.getOrNull(11)?.toDoubleOrNull() ?: 0.0
                    val bloodGlucose = tokens.getOrNull(12)?.toDoubleOrNull()
                    val targetGlucose = tokens.getOrNull(13)?.toDoubleOrNull()
                    val correctionFactor = tokens.getOrNull(14)?.toDoubleOrNull()
                    val correctionInsulin = tokens.getOrNull(15)?.toDoubleOrNull()
                    val totalInsulin = tokens.getOrNull(16)?.toDoubleOrNull() ?: mealInsulin
                    val roundedInsulin = tokens.getOrNull(17)?.toDoubleOrNull() ?: totalInsulin
                    val notes = tokens.getOrNull(18) ?: ""

                    CalculationLog(
                        id = id,
                        timestamp = timestamp,
                        mealTitle = mealTitle,
                        rawCarbInput = rawCarbInput,
                        carbUnit = carbUnit,
                        carbGrams = carbGrams,
                        beValue = beValue,
                        keValue = keValue,
                        timeOfDay = timeOfDay,
                        insulinFactor = insulinFactor,
                        mealInsulin = mealInsulin,
                        bloodGlucose = bloodGlucose,
                        targetGlucose = targetGlucose,
                        correctionFactor = correctionFactor,
                        correctionInsulin = correctionInsulin,
                        totalInsulin = totalInsulin,
                        roundedInsulin = roundedInsulin,
                        notes = notes
                    )
                } else if (tokens.size >= 11) {
                    // Simplified / Legacy format
                    val id = tokens.getOrNull(0)?.toLongOrNull() ?: 0L
                    val timestamp = tokens.getOrNull(1)?.toLongOrNull() ?: System.currentTimeMillis()
                    val mealTitle = tokens.getOrNull(2)?.ifBlank { "Mahlzeit" } ?: "Mahlzeit"
                    val rawCarbInput = tokens.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                    val carbUnit = tokens.getOrNull(4) ?: "g KH"
                    val carbGrams = tokens.getOrNull(5)?.toDoubleOrNull() ?: rawCarbInput
                    val beValue = tokens.getOrNull(6)?.toDoubleOrNull() ?: (carbGrams / 12.0)
                    val keValue = tokens.getOrNull(7)?.toDoubleOrNull() ?: (carbGrams / 10.0)
                    val timeOfDay = tokens.getOrNull(8) ?: "Mahlzeit"
                    val insulinFactor = tokens.getOrNull(9)?.toDoubleOrNull() ?: 1.0
                    val mealInsulin = tokens.getOrNull(10)?.toDoubleOrNull() ?: 0.0
                    val bloodGlucose = tokens.getOrNull(11)?.toDoubleOrNull()
                    val targetGlucose = tokens.getOrNull(12)?.toDoubleOrNull()
                    val correctionInsulin = tokens.getOrNull(13)?.toDoubleOrNull()
                    val roundedInsulin = tokens.getOrNull(14)?.toDoubleOrNull() ?: mealInsulin
                    val notes = tokens.getOrNull(15) ?: ""

                    CalculationLog(
                        id = id,
                        timestamp = timestamp,
                        mealTitle = mealTitle,
                        rawCarbInput = rawCarbInput,
                        carbUnit = carbUnit,
                        carbGrams = carbGrams,
                        beValue = beValue,
                        keValue = keValue,
                        timeOfDay = timeOfDay,
                        insulinFactor = insulinFactor,
                        mealInsulin = mealInsulin,
                        bloodGlucose = bloodGlucose,
                        targetGlucose = targetGlucose,
                        correctionFactor = null,
                        correctionInsulin = correctionInsulin,
                        totalInsulin = roundedInsulin,
                        roundedInsulin = roundedInsulin,
                        notes = notes
                    )
                } else {
                    null
                }

                if (log != null) {
                    parsedLogs.add(log)
                }
            } catch (_: Exception) {
                // Ignore corrupted single line and continue parsing the rest
            }
        }

        return parsedLogs
    }

    /**
     * Splits a CSV line handling quoted strings and escaped double quotes.
     */
    fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    private fun escapeCsv(value: String): String {
        var v = value.replace("\"", "\"\"")
        if (v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"")) {
            v = "\"$v\""
        }
        return v
    }

    /**
     * Safely reads text from an Android content URI.
     */
    suspend fun readTextFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Safely writes text content to an Android content URI.
     */
    suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Auto-detects whether the content of the file is JSON or CSV and imports accordingly.
     */
    suspend fun importFromUri(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = readTextFromUri(context, uri)
        if (text.isNullOrBlank()) {
            return@withContext ImportResult(
                success = false,
                message = "The selected file is empty or could not be read."
            )
        }

        val trimmed = text.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            // Attempt JSON import
            val jsonResult = importFromJson(context, trimmed)
            if (jsonResult.success) {
                return@withContext jsonResult
            }
        }

        // Attempt CSV import
        val csvResult = importFromCsv(context, trimmed)
        if (csvResult.success) {
            return@withContext csvResult
        }

        ImportResult(
            success = false,
            message = "Unrecognized or corrupted file format. Please choose a valid JSON or CSV backup file."
        )
    }
}
