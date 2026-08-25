package network.spiritscorp

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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import network.spiritscorp.data.CalculationLogDao
import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.data.UserSettingsDao
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * End-to-end system and integration scenario tests covering full user journeys:
 * - Multi-meal logging and daily statistics
 * - Customization, backup export, wipe, and complete restore
 * - High-correction bolus scenarios and CSV/JSON reporting
 */
class InsulinCalculatorSystemScenarioTest {

    private lateinit var fakeLogDao: FakeCalculationLogDao
    private lateinit var fakeSettingsDao: FakeUserSettingsDao
    private lateinit var repository: InsulinRepository

    @Before
    fun setup() {
        fakeLogDao = FakeCalculationLogDao()
        fakeSettingsDao = FakeUserSettingsDao()
        repository = InsulinRepository(fakeLogDao, fakeSettingsDao)
    }

    @Test
    fun testFullDayDiabetesManagementJourney() = runTest {
        // Step 1: User configures insulin parameters
        val userSettings = UserSettings(
            morningFactor = 1.6,
            noonFactor = 1.0,
            eveningFactor = 1.3,
            nightFactor = 0.8,
            defaultCarbUnit = "g KH",
            beGramsDivisor = 12,
            targetGlucoseMgDl = 100.0,
            correctionFactorMgDl = 40.0,
            roundingStep = 0.5
        )
        repository.saveSettings(userSettings)

        // Step 2: Morning Breakfast (50g KH, BG = 110 mg/dl)
        // 50g / 12 = 4.17 BE. 4.17 * 1.6 = 6.67 IE. BG 110 -> (110-100)/40 = +0.25 IE. Total = 6.92 IE -> 7.0 IE
        val breakfastLog = CalculationLog(
            id = 1L,
            timestamp = System.currentTimeMillis() - (10 * 3600 * 1000),
            mealTitle = "Frühstück: Haferflocken mit Beeren",
            rawCarbInput = 50.0,
            carbUnit = "g KH",
            carbGrams = 50.0,
            beValue = 4.17,
            keValue = 5.0,
            timeOfDay = TimeOfDay.MORNING.title,
            insulinFactor = 1.6,
            mealInsulin = 6.67,
            bloodGlucose = 110.0,
            targetGlucose = 100.0,
            correctionFactor = 40.0,
            correctionInsulin = 0.25,
            totalInsulin = 6.92,
            roundedInsulin = 7.0,
            notes = "Vor der Arbeit"
        )
        repository.saveCalculation(breakfastLog)

        // Step 3: Lunch (4.0 BE, BG = 180 mg/dl - elevated)
        // 4 BE * 12 = 48g KH. 4 * 1.0 = 4.0 IE. BG 180 -> (180-100)/40 = +2.0 IE. Total = 6.0 IE
        val lunchLog = CalculationLog(
            id = 2L,
            timestamp = System.currentTimeMillis() - (5 * 3600 * 1000),
            mealTitle = "Mittagessen: Reisgericht",
            rawCarbInput = 4.0,
            carbUnit = "BE",
            carbGrams = 48.0,
            beValue = 4.0,
            keValue = 4.8,
            timeOfDay = TimeOfDay.NOON.title,
            insulinFactor = 1.0,
            mealInsulin = 4.0,
            bloodGlucose = 180.0,
            targetGlucose = 100.0,
            correctionFactor = 40.0,
            correctionInsulin = 2.0,
            totalInsulin = 6.0,
            roundedInsulin = 6.0,
            notes = "Kantine"
        )
        repository.saveCalculation(lunchLog)

        // Step 4: Dinner (30g KH, BG = 95 mg/dl - in range)
        // 30g / 12 = 2.5 BE. 2.5 * 1.3 = 3.25 IE. BG 95 -> 0 corr. Total = 3.25 IE -> 3.5 IE
        val dinnerLog = CalculationLog(
            id = 3L,
            timestamp = System.currentTimeMillis() - (1 * 3600 * 1000),
            mealTitle = "Abendessen: Vollkornbrot & Salat",
            rawCarbInput = 30.0,
            carbUnit = "g KH",
            carbGrams = 30.0,
            beValue = 2.5,
            keValue = 3.0,
            timeOfDay = TimeOfDay.EVENING.title,
            insulinFactor = 1.3,
            mealInsulin = 3.25,
            bloodGlucose = 95.0,
            targetGlucose = 100.0,
            correctionFactor = 40.0,
            correctionInsulin = 0.0,
            totalInsulin = 3.25,
            roundedInsulin = 3.5,
            notes = "Zuhause"
        )
        repository.saveCalculation(dinnerLog)

        // Step 5: Verify Logbook state & total statistics
        val allLogs = repository.getAllLogsDirect()
        assertEquals(3, allLogs.size)

        val totalCarbsToday = allLogs.sumOf { it.carbGrams }
        assertEquals(128.0, totalCarbsToday, 0.001)

        val totalInsulinToday = allLogs.sumOf { it.roundedInsulin }
        assertEquals(16.5, totalInsulinToday, 0.001)

        // Step 6: Export to CSV and verify all 3 meals are included
        val csv = DatabaseBackupManager.exportToCsv(allLogs)
        assertTrue(csv.contains("Frühstück: Haferflocken mit Beeren"))
        assertTrue(csv.contains("Mittagessen: Reisgericht"))
        assertTrue(csv.contains("Abendessen: Vollkornbrot & Salat"))
        assertTrue(csv.contains("128.0") || csv.contains("50.0") && csv.contains("48.0") && csv.contains("30.0"))

        // Step 7: Export to JSON Backup
        val settings = repository.getSettings()
        val jsonBackup = DatabaseBackupManager.exportToJson(settings, allLogs)
        assertNotNull(jsonBackup)

        // Step 8: Simulate clearing database (e.g. device switch / reset)
        repository.clearLogs()
        assertEquals(0, repository.getAllLogsDirect().size)

        // Step 9: Restore database from JSON Backup
        val (restoredSettings, restoredLogs) = DatabaseBackupManager.parseJson(jsonBackup)!!
        assertNotNull(restoredSettings)
        assertEquals(1.6, restoredSettings!!.morningFactor, 0.001)

        repository.saveSettings(restoredSettings)
        repository.saveLogs(restoredLogs)

        val restoredDbLogs = repository.getAllLogsDirect()
        assertEquals(3, restoredDbLogs.size)
        assertEquals(128.0, restoredDbLogs.sumOf { it.carbGrams }, 0.001)
        assertEquals(16.5, restoredDbLogs.sumOf { it.roundedInsulin }, 0.001)
    }

    @Test
    fun testAiMealEstimationToBolusCalculationIntegration() {
        // Given estimated meal from AI
        val estimatedMealName = "Pizza Margherita mit Rucola"
        val estimatedCarbsGrams = 85.0

        // User is in BE mode
        val unit = CarbUnit.BE
        val rawUnitsInput = unit.fromGrams(estimatedCarbsGrams) // 85g / 12 = 7.08 BE
        val factor = 1.20 // Evening factor

        val mealInsulin = rawUnitsInput * factor // 7.0833 * 1.2 = 8.5 IE

        // Patient has BG = 160 mg/dl, target 100, corrFactor 40 -> +1.5 IE
        val correctionInsulin = (160.0 - 100.0) / 40.0
        val totalInsulin = mealInsulin + correctionInsulin // 8.5 + 1.5 = 10.0 IE

        val roundedTotal = BigDecimal(totalInsulin).setScale(1, RoundingMode.HALF_UP).toDouble()

        val log = CalculationLog(
            mealTitle = estimatedMealName,
            rawCarbInput = rawUnitsInput,
            carbUnit = unit.shortName,
            carbGrams = estimatedCarbsGrams,
            beValue = estimatedCarbsGrams / 12.0,
            keValue = estimatedCarbsGrams / 10.0,
            timeOfDay = "Abends",
            insulinFactor = factor,
            mealInsulin = mealInsulin,
            bloodGlucose = 160.0,
            targetGlucose = 100.0,
            correctionFactor = 40.0,
            correctionInsulin = correctionInsulin,
            totalInsulin = totalInsulin,
            roundedInsulin = roundedTotal,
            notes = "KI-geschätzte Mahlzeit (Fettverzögerung beachten)"
        )

        assertEquals("Pizza Margherita mit Rucola", log.mealTitle)
        assertEquals(85.0, log.carbGrams, 0.001)
        assertEquals(10.0, log.roundedInsulin, 0.001)
        assertTrue(log.notes.contains("KI-geschätzte"))
    }

    // --- Fake DAO Implementations ---

    private class FakeCalculationLogDao : CalculationLogDao {
        private val logs = mutableListOf<CalculationLog>()
        private val _flow = MutableStateFlow<List<CalculationLog>>(emptyList())
        private var nextId = 1L

        private fun updateFlow() {
            _flow.value = logs.sortedByDescending { it.timestamp }.toList()
        }

        override fun getAllLogs(): Flow<List<CalculationLog>> = _flow.asStateFlow()

        override suspend fun getAllLogsDirect(): List<CalculationLog> {
            return logs.sortedByDescending { it.timestamp }.toList()
        }

        override suspend fun insertLog(log: CalculationLog): Long {
            val assignedId = if (log.id == 0L) nextId++ else log.id
            val toSave = log.copy(id = assignedId)
            logs.removeAll { it.id == assignedId }
            logs.add(toSave)
            updateFlow()
            return assignedId
        }

        override suspend fun insertLogs(logsToInsert: List<CalculationLog>): List<Long> {
            val ids = mutableListOf<Long>()
            for (l in logsToInsert) {
                ids.add(insertLog(l))
            }
            return ids
        }

        override suspend fun deleteLogById(logId: Long) {
            logs.removeAll { it.id == logId }
            updateFlow()
        }

        override suspend fun clearAllLogs() {
            logs.clear()
            updateFlow()
        }
    }

    private class FakeUserSettingsDao : UserSettingsDao {
        private var storedSettings: UserSettings? = null
        private val _flow = MutableStateFlow<UserSettings?>(null)

        override fun getSettings(): Flow<UserSettings?> = _flow.asStateFlow()

        override suspend fun getSettingsDirect(): UserSettings? = storedSettings

        override suspend fun saveSettings(settings: UserSettings) {
            storedSettings = settings
            _flow.value = settings
        }
    }
}
