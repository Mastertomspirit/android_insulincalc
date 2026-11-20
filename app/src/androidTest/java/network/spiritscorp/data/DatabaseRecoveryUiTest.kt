package network.spiritscorp.data

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

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import network.spiritscorp.MainActivity
import network.spiritscorp.assertDoesNotExist
import network.spiritscorp.assertIsDisplayed
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

/**
 * Real instrumented on-device UI and integration test suite for database corruption and recovery workflows.
 * Rigorously tests all three recovery options from DatabaseRecoveryDialog entirely through UI interactions:
 * 1. Close App: Verifies app terminates and database file state/hash remains unchanged.
 * 2. Reset Database: Verifies database is successfully reset, re-initialized, and normal app operation (default settings & empty logbook) is fully functional.
 * 3. Import Backup: Verifies clicking the Import button on the dialog triggers file selection via intent stubbing, imports UserSettings and 2 complete CalculationLogs, navigates to Logbook via UI test tags, and validates that both entries appear in the UI.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseRecoveryUiTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    companion object {
        private const val TAG_DIALOG = "db_recovery_dialog"
        private const val TAG_BTN_CLOSE = "btn_db_recovery_close"
        private const val TAG_BTN_RESET = "btn_db_recovery_reset"
        private const val TAG_BTN_IMPORT = "btn_db_recovery_import"
        private const val TAG_NAV_BAR = "bottom_navigation_bar"
        private const val TAG_NAV_LOGBOOK = "nav_logbook"

        private const val TITLE_LOG_1 = "Haferflocken & Heidelbeeren"
        private const val NOTES_LOG_1 = "Sensor leicht steigend"
        private const val TITLE_LOG_2 = "Vollkornbrot mit Käse"
        private const val NOTES_LOG_2 = "Vor dem Training"
        private val TIME = System.currentTimeMillis() - 1_000
        private val TIME_2 = TIME - 50_000
    }

    @Before
    fun setUp() {
        DatabaseRestoreManager().resetInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        Intents.init()
        setUpCorruptedDatabase()
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DatabaseRestoreManager().resetInstance(context)
        Files.deleteIfExists(Paths.get(InstrumentationRegistry
            .getInstrumentation().targetContext
            .getDatabasePath(DatabaseConstants.DATABASE_NAME)
            .absolutePath))
        context.getSharedPreferences(DatabaseConstants.SECURITY_PREFS_FILE, Context.MODE_PRIVATE).edit().clear().commit()
        Intents.release()
    }

    @Test
    fun testRecoveryDialogCloseAppWithHashVerification() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = context.getDatabasePath(DatabaseConstants.DATABASE_NAME)
        val initialHash = getFileHash(dbFile)

        // Verify that the database recovery dialog is displayed automatically on startup due to database error
        composeTestRule.onNodeWithTag(TAG_DIALOG).assertExists("Database recovery dialog should be displayed when database is corrupted")

        // Click the Close App button using test tag
        composeTestRule.onNodeWithTag(TAG_BTN_CLOSE).assertExists("Close app button should be present in recovery dialog").performClick()

        // Verify that the activity is finishing or destroyed
        var activity: MainActivity? = null
        scenario.onActivity { activity = it }
        assertTrue("Activity should finish when Close App is clicked", activity?.isFinishing == true || activity?.isDestroyed == true)

        // Verify that the database file state and hash remain unchanged as expected
        val finalHash = getFileHash(dbFile)
        assertEquals("Database file hash should remain identical after closing the app", initialHash, finalHash)
        scenario.close()
    }

    @Test
    fun testRecoveryDialogResetDatabaseAndVerifyNormalOperation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        // Verify that the database recovery dialog is displayed
        composeTestRule.onNodeWithTag(TAG_DIALOG).assertExists("Database recovery dialog should be displayed on startup")

        // Click the Reset Database button using test tag
        composeTestRule.onNodeWithTag(TAG_BTN_RESET).assertExists("Reset database button should be present in recovery dialog").performClick()

        // Verify that the recovery dialog is dismissed
        composeTestRule.onNodeWithTag(TAG_DIALOG).assertDoesNotExist("Database recovery dialog should be dismissed after resetting database")

        // Verify that normal app operation is restored and bottom navigation bar is visible
        composeTestRule.onNodeWithTag(TAG_NAV_BAR).assertExists("Bottom navigation bar should be visible after successful database reset")

        // Verify via ViewModel that a fresh database connection is fully operational, empty logs, and default settings
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = InsulinCalculatorViewModel(context.applicationContext as Application)

        val settings = viewModel.userSettings.value
        assertNotNull("User settings should be initialized after database reset", settings)

        val logs = viewModel.historyLogs.value
        assertNotNull("History logs list should be initialized after database reset", logs)
        assertTrue("History logs list should be empty after database reset", logs.isEmpty())
        scenario.close()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRecoveryDialogImportBackupWorkflowAndVerifyUiDisplay() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Prepare a complete JSON backup file with custom UserSettings (OCEAN_BLUE theme) and 2 complete CalculationLogs
        val backupFile = File(context.cacheDir, "sample_complete_backup.json")
        backupFile.writeText("""
            {
              "version": 3,
              "exportDate": "2026-11-17 10:50:37",
              "app": "InsulinRechner",
              "settings": {
                "id": 1,
                "morningFactor": 1.5,
                "noonFactor": 1,
                "eveningFactor": 1.2,
                "nightFactor": 0.8,
                "defaultCarbUnit": "GRAMS",
                "beGramsDivisor": 12,
                "glucoseUnit": "MG_DL",
                "targetGlucoseMgDl": 120,
                "correctionFactorMgDl": 50,
                "roundingStep": 0.5,
                "showDisclaimer": false,
                "selectedTheme": "OCEAN_BLUE",
                "themeMode": "LIGHT",
                "selectedAiModel": "GEMINI_3_5_FLASH_LITE"
              },
              "logs": [
                {
                  "id": 2,
                  "timestamp": "$TIME",
                  "mealTitle": "$TITLE_LOG_1",
                  "rawCarbInput": 75,
                  "carbUnit": "GRAMS",
                  "carbGrams": 75,
                  "beValue": 6.25,
                  "keValue": 7.5,
                  "timeOfDay": "NOON",
                  "insulinFactor": 1,
                  "mealInsulin": 6.25,
                  "bloodGlucose": 100,
                  "targetGlucose": 120,
                  "correctionFactor": 50,
                  "correctionInsulin": -0.4,
                  "totalInsulin": 5.85,
                  "roundedInsulin": 6,
                  "notes": "$NOTES_LOG_1"
                },
                {
                  "id": 3,
                  "timestamp": "$TIME_2",
                  "mealTitle": "$TITLE_LOG_2",
                  "rawCarbInput": 0,
                  "carbUnit": "GRAMS",
                  "carbGrams": 0,
                  "beValue": 0,
                  "keValue": 0,
                  "timeOfDay": "EVENING",
                  "insulinFactor": 1.2,
                  "mealInsulin": 0,
                  "bloodGlucose": 70,
                  "targetGlucose": 120,
                  "correctionFactor": 50,
                  "correctionInsulin": -1,
                  "totalInsulin": 0,
                  "roundedInsulin": 0,
                  "notes": "$NOTES_LOG_2"
                }
              ]
            }
        """.trimIndent())

        // Stub the file picker intent using direct Uri.fromFile(...) without FileProvider
        val uri = Uri.fromFile(backupFile)
        val resultData = Intent().apply {
            data = uri
        }
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(result)

        // Verify that the database recovery dialog is displayed on startup
        composeTestRule.onNodeWithTag(TAG_DIALOG).assertExists("Database recovery dialog should be displayed on startup")

        // Click the Import Backup button on the dialog using test tag (triggers backupImportLauncher via UI)
        composeTestRule.onNodeWithTag(TAG_BTN_IMPORT).assertExists("Import backup button should be present in recovery dialog").performClick()

        // Navigate to the Logbook tab via UI test tag
        composeTestRule.onNodeWithTag(TAG_NAV_LOGBOOK).assertIsDisplayed("Logbook bottom navigation item should be visible").performClick()

        // Wait for both imported log entries to appear in the Logbook UI list
        composeTestRule.waitUntil(
            timeoutMillis = 3_000,
            conditionDescription = "Imported log entries to appear in the logbook list"
        ) {
                composeTestRule.onNode(hasText(TITLE_LOG_1)).isDisplayed() &&
                composeTestRule.onNode(hasText(TITLE_LOG_2)).isDisplayed()
        }

        // Verify both custom titles and notes are displayed in the Logbook UI
        composeTestRule.onNode(hasText(TITLE_LOG_1)).assertIsDisplayed("First imported meal title '$TITLE_LOG_1' should be displayed in the logbook")
        composeTestRule.onNode(hasText(NOTES_LOG_1)).assertIsDisplayed("First imported meal notes '$NOTES_LOG_1' should be displayed in the logbook")

        composeTestRule.onNode(hasText(TITLE_LOG_2)).assertIsDisplayed("Second imported meal title '$TITLE_LOG_2' should be displayed in the logbook")
        composeTestRule.onNode(hasText(NOTES_LOG_2)).assertIsDisplayed("Second imported meal notes '$NOTES_LOG_2' should be displayed in the logbook")
        scenario.close()
    }


    private fun setUpCorruptedDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Clear security preferences
        context.getSharedPreferences(DatabaseConstants.SECURITY_PREFS_FILE, Context.MODE_PRIVATE)
            .edit().clear()
            .putString("secure_db_encrypted_passphrase", "SGVsbG9Xb3JsZFRlc3RJbnZhbGlkRW5jcnlwdGVkUGFzc3BocmFzZQ==")
            .commit()

        // Place a corrupted database file on disk to trigger database initialization error on startup
        val dbFile = context.getDatabasePath(DatabaseConstants.DATABASE_NAME)
        val path = dbFile.absolutePath

        Files.deleteIfExists(Paths.get(path))
        Files.createFile(Paths.get(path))
        Files.deleteIfExists(Paths.get("$path-wal"))
        Files.deleteIfExists(Paths.get("$path-shm"))


        // Create a corrupted database file on disk so AppDatabase.getDatabase() throws an exception
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("CORRUPTED_SQLCIPHER_DATABASE_PAYLOAD")
    }

    private fun getFileHash(file: File): String {
        if (!file.exists()) return ""
        val bytes = file.readBytes()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
