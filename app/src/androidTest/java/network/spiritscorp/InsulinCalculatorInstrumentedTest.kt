package network.spiritscorp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import network.spiritscorp.data.AppDatabase
import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.CarbUnit
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.TimeOfDay
import network.spiritscorp.model.UserSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-Device / Instrumentation Test Suite for Android execution environments.
 * 
 * Verifies Android Context binding, package identifier, database instantiation
 * on target device, and Android-level serialization / backup operations.
 */
@RunWith(AndroidJUnit4::class)
class InsulinCalculatorInstrumentedTest {

  @Test
  fun testAppContextAndPackageIdentity() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertNotNull(appContext)
    assertEquals("network.spiritscorp.insulincalc", appContext.packageName)
  }

  @Test
  fun testRoomDatabaseInstanceCreationOnDevice() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val database = AppDatabase.getDatabase(appContext)
    assertNotNull(database)
    assertNotNull(database.calculationLogDao())
    assertNotNull(database.userSettingsDao())
  }

  @Test
  fun testRepositoryAndDeviceBackupWorkflow() = runBlocking {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val database = AppDatabase.getDatabase(appContext)
    val repository = InsulinRepository(database.calculationLogDao(), database.userSettingsDao())

    val settings = repository.getSettings()
    assertNotNull(settings)
    assertTrue(settings.morningFactor > 0.0)

    val sampleLog = CalculationLog(
      mealTitle = "Test On-Device Mahlzeit",
      rawCarbInput = 40.0,
      carbUnit = CarbUnit.GRAMS.shortName,
      carbGrams = 40.0,
      beValue = 3.33,
      keValue = 4.0,
      timeOfDay = TimeOfDay.NOON.title,
      insulinFactor = 1.0,
      mealInsulin = 3.33,
      totalInsulin = 3.33,
      roundedInsulin = 3.5
    )

    val json = DatabaseBackupManager.exportToJson(settings, listOf(sampleLog))
    assertTrue(json.contains("Test On-Device Mahlzeit"))
    assertTrue(json.contains("settings"))

    val parsed = DatabaseBackupManager.parseJson(json)
    assertNotNull(parsed)
    assertEquals(1, parsed!!.second.size)
    assertEquals("Test On-Device Mahlzeit", parsed.second[0].mealTitle)
  }
}
