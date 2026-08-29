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
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.model.UserSettings

/**
 * Primary Room Database for the Insulin Calculator application.
 *
 * Security & Architecture Highlights:
 * - Backed by SQLCipher 256-bit AES database encryption.
 * - Hardware-backed passphrase generated and secured via Android KeyStore.
 * - Stores all patient calculation logs and user therapy configurations locally on device.
 */
@Database(
    entities = [CalculationLog::class, UserSettings::class],
    version = 1,
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
         * Retrieves the thread-safe singleton instance of [AppDatabase].
         * Initializes SQLCipher native libraries, obtains or generates the 256-bit AES encryption key,
         * and configures the encrypted Room instance directly at version 1.
         *
         * @param context Application context.
         * @return Initialized, secure [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext

                // Step 1: Initialize SQLCipher native binaries
                try {
                    System.loadLibrary("sqlcipher")
                } catch (_: Throwable) {
                    // Handled gracefully in JVM test environments
                }

                // Step 2: Retrieve or generate hardware-secured 256-bit encryption key
                val passphrase = DatabaseSecurityManager.getOrCreateDatabasePassphrase(appContext)

                // Step 3: Configure Room directly with SQLCipher SupportOpenHelperFactory
                val supportFactory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(supportFactory)
                    .fallbackToDestructiveMigration(false)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
