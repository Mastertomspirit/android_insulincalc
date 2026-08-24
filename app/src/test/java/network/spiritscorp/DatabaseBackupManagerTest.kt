package network.spiritscorp

import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying complete database export and import functionality (JSON and CSV),
 * ensuring full logbook coverage and resilient error handling on corrupted data.
 */
class DatabaseBackupManagerTest {

    private fun createSampleLogs(): List<CalculationLog> {
        return listOf(
            CalculationLog(
                id = 1L,
                timestamp = 1700000000000L,
                mealTitle = "Frühstück (Müsli & Apfel)",
                rawCarbInput = 45.0,
                carbUnit = "g KH",
                carbGrams = 45.0,
                beValue = 3.75,
                keValue = 4.5,
                timeOfDay = "Morgens",
                insulinFactor = 1.5,
                mealInsulin = 6.75,
                bloodGlucose = 140.0,
                targetGlucose = 100.0,
                correctionFactor = 40.0,
                correctionInsulin = 1.0,
                totalInsulin = 7.75,
                roundedInsulin = 8.0,
                notes = "Haferflocken mit Apfel"
            ),
            CalculationLog(
                id = 2L,
                timestamp = 1700015000000L,
                mealTitle = "Mittagessen (Pasta, Tomatensauce)",
                rawCarbInput = 6.0,
                carbUnit = "BE",
                carbGrams = 72.0,
                beValue = 6.0,
                keValue = 7.2,
                timeOfDay = "Mittags",
                insulinFactor = 1.0,
                mealInsulin = 6.0,
                bloodGlucose = 110.0,
                targetGlucose = 100.0,
                correctionFactor = 40.0,
                correctionInsulin = 0.25,
                totalInsulin = 6.25,
                roundedInsulin = 6.5,
                notes = "Vollkornnudeln"
            ),
            CalculationLog(
                id = 3L,
                timestamp = 1700035000000L,
                mealTitle = "Abendessen (Brot mit Käse)",
                rawCarbInput = 3.5,
                carbUnit = "KE",
                carbGrams = 35.0,
                beValue = 2.92,
                keValue = 3.5,
                timeOfDay = "Abends",
                insulinFactor = 1.2,
                mealInsulin = 4.2,
                bloodGlucose = 95.0,
                targetGlucose = 100.0,
                correctionFactor = 40.0,
                correctionInsulin = 0.0,
                totalInsulin = 4.2,
                roundedInsulin = 4.0,
                notes = "Roggenbrot"
            ),
            CalculationLog(
                id = 4L,
                timestamp = 1700050000000L,
                mealTitle = "Spät-Snack",
                rawCarbInput = 15.0,
                carbUnit = "g KH",
                carbGrams = 15.0,
                beValue = 1.25,
                keValue = 1.5,
                timeOfDay = "Nachts",
                insulinFactor = 0.8,
                mealInsulin = 1.2,
                bloodGlucose = null,
                targetGlucose = null,
                correctionFactor = null,
                correctionInsulin = null,
                totalInsulin = 1.2,
                roundedInsulin = 1.0,
                notes = "Joghurt"
            )
        )
    }

    @Test
    fun testExportAllLogsToJsonAndImportBack() {
        val sampleLogs = createSampleLogs()
        val sampleSettings = UserSettings(
            morningFactor = 1.80,
            noonFactor = 1.10,
            eveningFactor = 1.35,
            nightFactor = 0.90,
            defaultCarbUnit = "BE",
            beGramsDivisor = 12,
            targetGlucoseMgDl = 110.0,
            correctionFactorMgDl = 45.0,
            roundingStep = 0.5,
            selectedTheme = "LAVENDER_PURPLE"
        )

        // 1. Export to JSON
        val jsonOutput = DatabaseBackupManager.exportToJson(sampleSettings, sampleLogs)
        assertNotNull(jsonOutput)
        assertTrue(jsonOutput.contains("\"settings\""))
        assertTrue(jsonOutput.contains("\"logs\""))
        assertTrue(jsonOutput.contains("Frühstück (Müsli & Apfel)"))
        assertTrue(jsonOutput.contains("Spät-Snack"))

        // 2. Parse back
        val parsed = DatabaseBackupManager.parseJson(jsonOutput)
        assertNotNull("Parsed result should not be null", parsed)

        val (parsedSettings, parsedLogs) = parsed!!

        // Verify Settings
        assertNotNull(parsedSettings)
        assertEquals(1.80, parsedSettings!!.morningFactor, 0.001)
        assertEquals(1.10, parsedSettings.noonFactor, 0.001)
        assertEquals("BE", parsedSettings.defaultCarbUnit)
        assertEquals("LAVENDER_PURPLE", parsedSettings.selectedTheme)

        // Verify ALL 4 logs were exported and restored
        assertEquals(sampleLogs.size, parsedLogs.size)

        for (i in sampleLogs.indices) {
            val original = sampleLogs[i]
            val restored = parsedLogs[i]
            assertEquals(original.mealTitle, restored.mealTitle)
            assertEquals(original.carbGrams, restored.carbGrams, 0.001)
            assertEquals(original.totalInsulin, restored.totalInsulin, 0.001)
            assertEquals(original.roundedInsulin, restored.roundedInsulin, 0.001)
            assertEquals(original.notes, restored.notes)
            assertEquals(original.bloodGlucose, restored.bloodGlucose)
        }
    }

    @Test
    fun testExportAllLogsToCsv() {
        val sampleLogs = createSampleLogs()
        val csvOutput = DatabaseBackupManager.exportToCsv(sampleLogs)

        assertNotNull(csvOutput)
        val lines = csvOutput.trim().lines().filter { it.isNotBlank() }

        // Header line + 4 logs = 5 lines total
        assertEquals(5, lines.size)
        assertTrue(lines[0].startsWith("ID,Timestamp,Date,MealTitle"))
        assertTrue(lines[1].contains("Frühstück (Müsli & Apfel)"))
        assertTrue(lines[2].contains("Mittagessen (Pasta, Tomatensauce)"))
        assertTrue(lines[3].contains("Abendessen (Brot mit Käse)"))
        assertTrue(lines[4].contains("Spät-Snack"))

        // Parse CSV back
        val parsedLogs = DatabaseBackupManager.parseCsv(csvOutput)
        assertEquals(sampleLogs.size, parsedLogs.size)

        assertEquals("Frühstück (Müsli & Apfel)", parsedLogs[0].mealTitle)
        assertEquals(45.0, parsedLogs[0].carbGrams, 0.001)
        assertEquals("Mittagessen (Pasta, Tomatensauce)", parsedLogs[1].mealTitle)
        assertEquals(72.0, parsedLogs[1].carbGrams, 0.001)
        assertEquals("Spät-Snack", parsedLogs[3].mealTitle)
    }

    @Test
    fun testCsvEscapingWithQuotesAndCommas() {
        val logsWithCommas = listOf(
            CalculationLog(
                id = 10L,
                mealTitle = "Pizza \"Speciale\", extra Käse",
                rawCarbInput = 80.0,
                carbUnit = "g KH",
                carbGrams = 80.0,
                beValue = 6.67,
                keValue = 8.0,
                timeOfDay = "Abends",
                insulinFactor = 1.2,
                mealInsulin = 9.6,
                totalInsulin = 9.6,
                roundedInsulin = 9.5,
                notes = "Mit Salami, Pilzen, und \"Knoblauch-Öl\""
            )
        )

        val csv = DatabaseBackupManager.exportToCsv(logsWithCommas)
        val parsed = DatabaseBackupManager.parseCsv(csv)

        assertEquals(1, parsed.size)
        assertEquals("Pizza \"Speciale\", extra Käse", parsed[0].mealTitle)
        assertEquals("Mit Salami, Pilzen, und \"Knoblauch-Öl\"", parsed[0].notes)
    }

    @Test
    fun testCorruptedJsonImportDoesNotCrash() {
        // Empty string
        assertNull(DatabaseBackupManager.parseJson(""))

        // Whitespace only
        assertNull(DatabaseBackupManager.parseJson("   \n\t  "))

        // Truncated/Invalid JSON
        assertNull(DatabaseBackupManager.parseJson("{\"settings\": { \"morningFactor\": "))
        assertNull(DatabaseBackupManager.parseJson("{not_valid_json}"))

        // Random binary/garbage data
        assertNull(DatabaseBackupManager.parseJson("0xDEADBEEF-Corrupted-Binary-Stream-%%%"))

        // Valid JSON with empty settings/logs should return non-null with empty list
        val emptyJson = "{\"version\": 1, \"settings\": {}, \"logs\": []}"
        val parsedEmpty = DatabaseBackupManager.parseJson(emptyJson)
        assertNotNull(parsedEmpty)
        assertEquals(0, parsedEmpty!!.second.size)
    }

    @Test
    fun testCorruptedCsvImportDoesNotCrash() {
        // Empty string
        val emptyResult = DatabaseBackupManager.parseCsv("")
        assertTrue(emptyResult.isEmpty())

        // Random string without commas
        val garbageResult = DatabaseBackupManager.parseCsv("Some random invalid non-csv text")
        assertTrue(garbageResult.isEmpty())

        // Partially broken rows mixed with valid rows
        val mixedCsv = """
            ID,Timestamp,Date,MealTitle,RawCarbInput,CarbUnit,CarbGrams,BE,KE,TimeOfDay,InsulinFactor,MealInsulin,BloodGlucose,TargetGlucose,CorrectionFactor,CorrectionInsulin,TotalInsulin,RoundedInsulin,Notes
            1,1700000000000,"2023-11-14 20:00:00","Salat",10.0,"g KH",10.0,0.83,1.0,"Abends",1.0,1.0,,,0.0,1.0,1.0,"Leicht"
            BrokenRowWithoutEnoughColumns
            2,1700000001000,"2023-11-14 21:00:00","Suppe",20.0,"g KH",20.0,1.67,2.0,"Abends",1.0,2.0,,,0.0,2.0,2.0,"Warm"
        """.trimIndent()

        val parsedMixed = DatabaseBackupManager.parseCsv(mixedCsv)
        // Should safely parse the 2 valid rows while gracefully skipping the broken row
        assertEquals(2, parsedMixed.size)
        assertEquals("Salat", parsedMixed[0].mealTitle)
        assertEquals("Suppe", parsedMixed[1].mealTitle)
    }

    @Test
    fun testJsonImportWithDirectArrayOfLogs() {
        val arrayJson = """
            [
              {
                "id": 5,
                "mealTitle": "Frühstücks-Smoothie",
                "carbGrams": 30.0,
                "rawCarbInput": 30.0,
                "carbUnit": "g KH",
                "timeOfDay": "Morgens",
                "insulinFactor": 1.5,
                "mealInsulin": 4.5,
                "totalInsulin": 4.5,
                "roundedInsulin": 4.5
              }
            ]
        """.trimIndent()

        val parsed = DatabaseBackupManager.parseJson(arrayJson)
        assertNotNull(parsed)
        val (settings, logs) = parsed!!
        assertNull(settings) // Settings was not provided
        assertEquals(1, logs.size)
        assertEquals("Frühstücks-Smoothie", logs[0].mealTitle)
        assertEquals(30.0, logs[0].carbGrams, 0.001)
    }

    @Test
    fun testSplitCsvLineHelper() {
        val line = "1,1700000000000,\"2023-11-14 20:00:00\",\"Pizza, Pasta & \"\"Vino\"\"\",50.0"
        val tokens = DatabaseBackupManager.splitCsvLine(line)

        assertEquals(5, tokens.size)
        assertEquals("1", tokens[0])
        assertEquals("1700000000000", tokens[1])
        assertEquals("2023-11-14 20:00:00", tokens[2])
        assertEquals("Pizza, Pasta & \"Vino\"", tokens[3])
        assertEquals("50.0", tokens[4])
    }
}
