package network.spiritscorp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import network.spiritscorp.data.CalculationLogDao
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.data.UserSettingsDao
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests verifying the interaction between the data access layer,
 * reactive Flows, suspend queries, and the InsulinRepository domain coordinator.
 */
class InsulinRepositoryIntegrationTest {

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
    fun testDefaultSettingsCreatedWhenNoneExist() = runTest {
        val settings = repository.getSettings()
        assertNotNull(settings)
        assertEquals(1.50, settings.morningFactor, 0.001)
        assertEquals("GRAMS", settings.defaultCarbUnit)
        assertEquals(120.0, settings.targetGlucoseMgDl, 0.001)
        assertEquals(50.0, settings.correctionFactorMgDl, 0.001)

        // Verify it was persisted
        val direct = fakeSettingsDao.getSettingsDirect()
        assertNotNull(direct)
        assertEquals(1.50, direct!!.morningFactor, 0.001)
    }

    @Test
    fun testSaveAndRetrieveCustomSettings() = runTest {
        val custom = UserSettings(
            morningFactor = 1.75,
            noonFactor = 1.15,
            eveningFactor = 1.40,
            nightFactor = 0.85,
            defaultCarbUnit = "BE",
            beGramsDivisor = 12,
            targetGlucoseMgDl = 105.0,
            correctionFactorMgDl = 45.0,
            roundingStep = 0.5,
            selectedTheme = "AMBER_WARM"
        )

        repository.saveSettings(custom)

        val retrieved = repository.getSettings()
        assertEquals(1.75, retrieved.morningFactor, 0.001)
        assertEquals("BE", retrieved.defaultCarbUnit)
        assertEquals("AMBER_WARM", retrieved.selectedTheme)

        val flowValue = repository.settingsFlow.first()
        assertEquals("AMBER_WARM", flowValue?.selectedTheme)
    }

    @Test
    fun testSaveSingleCalculationLogAndObserveFlow() = runTest {
        val log = CalculationLog(
            mealTitle = "Mittagessen (Reis mit Hühnchen)",
            rawCarbInput = 60.0,
            carbUnit = "g KH",
            carbGrams = 60.0,
            beValue = 5.0,
            keValue = 6.0,
            timeOfDay = "Mittags",
            insulinFactor = 1.0,
            mealInsulin = 5.0,
            bloodGlucose = 125.0,
            targetGlucose = 100.0,
            correctionFactor = 40.0,
            correctionInsulin = 0.63,
            totalInsulin = 5.63,
            roundedInsulin = 5.5,
            notes = "Leichte Sporteinheit danach"
        )

        val id = repository.saveCalculation(log)
        assertTrue(id > 0)

        val logsFromFlow = repository.allLogs.first()
        assertEquals(1, logsFromFlow.size)
        assertEquals("Mittagessen (Reis mit Hühnchen)", logsFromFlow[0].mealTitle)
        assertEquals(5.5, logsFromFlow[0].roundedInsulin, 0.001)

        val directLogs = repository.getAllLogsDirect()
        assertEquals(1, directLogs.size)
    }

    @Test
    fun testSaveMultipleLogsBatchAndOrdering() = runTest {
        val log1 = CalculationLog(id = 1L, timestamp = 1000L, mealTitle = "Mahlzeit 1")
        val log2 = CalculationLog(id = 2L, timestamp = 2000L, mealTitle = "Mahlzeit 2")
        val log3 = CalculationLog(id = 3L, timestamp = 3000L, mealTitle = "Mahlzeit 3")

        val insertedIds = repository.saveLogs(listOf(log1, log2, log3))
        assertEquals(3, insertedIds.size)

        val allLogs = repository.getAllLogsDirect()
        assertEquals(3, allLogs.size)
        // Check order descending by timestamp
        assertEquals("Mahlzeit 3", allLogs[0].mealTitle)
        assertEquals("Mahlzeit 2", allLogs[1].mealTitle)
        assertEquals("Mahlzeit 1", allLogs[2].mealTitle)
    }

    @Test
    fun testDeleteLogById() = runTest {
        val log1 = CalculationLog(id = 10L, timestamp = 1000L, mealTitle = "Frühstück")
        val log2 = CalculationLog(id = 20L, timestamp = 2000L, mealTitle = "Abendessen")

        repository.saveLogs(listOf(log1, log2))
        assertEquals(2, repository.getAllLogsDirect().size)

        repository.deleteLog(10L)
        val remaining = repository.getAllLogsDirect()
        assertEquals(1, remaining.size)
        assertEquals("Abendessen", remaining[0].mealTitle)
    }

    @Test
    fun testClearAllLogs() = runTest {
        val log1 = CalculationLog(id = 1L, timestamp = 1000L, mealTitle = "Eintrag 1")
        val log2 = CalculationLog(id = 2L, timestamp = 2000L, mealTitle = "Eintrag 2")
        repository.saveLogs(listOf(log1, log2))

        assertEquals(2, repository.getAllLogsDirect().size)

        repository.clearLogs()
        assertEquals(0, repository.getAllLogsDirect().size)
        assertEquals(0, repository.allLogs.first().size)
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

        override suspend fun insertLogs(logs: List<CalculationLog>): List<Long> {
            val ids = mutableListOf<Long>()
            for (l in logs) {
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
