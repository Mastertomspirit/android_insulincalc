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

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings

/**
 * Primary Room Database for the Insulin Calculator application.
 *
 * Security & Architecture Highlights:
 * - Backed by SQLCipher 256-bit AES database encryption.
 * - Passphrase is automatically generated and secured via Android KeyStore / Jetpack Security.
 * - Automatically detects and migrates legacy unencrypted database files upon first launch.
 * - Stores all patient calculation logs and user therapy configurations locally on device.
 */
@Database(
    entities = [CalculationLog::class, UserSettings::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Data access object for querying and persisting [CalculationLog] entries.
     */
    abstract fun calculationLogDao(): CalculationLogDao

    /**
     * Data access object for managing personalized [UserSettings] therapy factors and preferences.
     */
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        private const val DATABASE_NAME = "insulin_calculator.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from database version 1 to version 2.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 to 2 schema adjustments
            }
        }

        /**
         * Migration from database version 2 to version 3:
         * Adds BE / KE carbohydrate columns to calculation logs and customizable divisor settings.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add BE and KE values to historical logs table
                db.execSQL("ALTER TABLE calculation_logs ADD COLUMN beValue REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE calculation_logs ADD COLUMN keValue REAL NOT NULL DEFAULT 0.0")
                
                // Add custom BE divisor and medical disclaimer preference to user settings table
                db.execSQL("ALTER TABLE user_settings ADD COLUMN beGramsDivisor INTEGER NOT NULL DEFAULT 12")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN showDisclaimer INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * Retrieves the thread-safe singleton instance of [AppDatabase].
         * Initializes SQLCipher native libraries, obtains or generates the 256-bit AES encryption key,
         * transparently encrypts any legacy plaintext database files, and configures the encrypted Room instance.
         *
         * @param context Application context.
         * @return Initialized, secure [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext

                // Step 1: Initialize SQLCipher native binaries
                try {
                    SQLiteDatabase.loadLibs(appContext)
                } catch (_: UnsatisfiedLinkError) {
                    // Handled gracefully in JVM test environments
                }

                // Step 2: Retrieve or generate hardware-secured 256-bit encryption key
                val passphrase = DatabaseSecurityManager.getOrCreateDatabasePassphrase(appContext)

                // Step 3: Transparently migrate any existing unencrypted database to encrypted SQLCipher
                try {
                    DatabaseSecurityManager.ensureDatabaseEncrypted(appContext, DATABASE_NAME, passphrase)
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error ensuring database encryption: ${e.message}", e)
                }

                // Step 4: Configure Room with SQLCipher SupportFactory
                val supportFactory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(supportFactory)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
