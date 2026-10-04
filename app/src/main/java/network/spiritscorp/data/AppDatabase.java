package network.spiritscorp.data;

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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.UserSettings;
import network.spiritscorp.model.GlucoseUnit;
import network.spiritscorp.util.AppConstants;

/**
 * Primary Room Database for the Insulin Calculator application written in Java.
 *<br><br>
 * Security & Architecture Highlights:<br>
 * - Backed by SQLCipher 256-bit AES database encryption.<br>
 * - Hardware-backed passphrase generated and secured via Android KeyStore.<br>
 * - Stores all patient calculation logs and user therapy configurations locally on device.<br>
 */
@Database(
        entities = {CalculationLog.class, UserSettings.class},
        version = AppConstants.DATABASE_VERSION,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = AppConstants.DATABASE_NAME;
    private static volatile AppDatabase INSTANCE;

    /**
     * Data access object for querying and persisting {@link CalculationLog} entries.
     */
    public abstract CalculationLogDao calculationLogDao();

    /**
     * Data access object for managing personalized {@link UserSettings} therapy factors and preferences.
     */
    public abstract UserSettingsDao userSettingsDao();

    /**
     * Migrates the database from version 1 to version 2.
     *
     * <p>The {@code glucoseUnit} column was previously stored as a human-readable
     * string such as {@code "mg/dl"} or {@code "mmol/l"}. Room now maps the
     * {@link GlucoseUnit} enum automatically using the enum constant name, so
     * existing values must be converted to {@code "MG_DL"} and {@code "MMOL_L"}.</p>
     */

    /**
     * Retrieves the thread-safe singleton instance of {@link AppDatabase}.
     *
     * @param context Application context.
     * @return Initialized, secure {@link AppDatabase} instance.
     */
    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();

                    // Step 1: Initialize SQLCipher native binaries
                    try {
                        System.loadLibrary("sqlcipher");
                    } catch (Throwable ignored) {
                        // Handled gracefully in JVM test environments
                    }

                    // Step 2: Retrieve or generate hardware-secured 256-bit encryption key
                    DatabaseSecurityManager securityManager = new DatabaseSecurityManager(appContext);
                    byte[] passphrase = securityManager.getOrCreateDatabasePassphrase();

                    // Step 3: Configure Room directly with SQLCipher SupportOpenHelperFactory
                    SupportOpenHelperFactory supportFactory = new SupportOpenHelperFactory(passphrase);

                    final Migration MIGRATION_1_2 = new Migration(1, 2) {

                        @Override
                        public void migrate(@NonNull SupportSQLiteDatabase database) {
                            // Convert glucose units to enum names
                            database.execSQL(
                                    "UPDATE user_settings " +
                                            "SET glucoseUnit = CASE glucoseUnit " +
                                            "WHEN 'mg/dl' THEN 'MG_DL' " +
                                            "WHEN 'mmol/l' THEN 'MMOL_L' " +
                                            "ELSE 'MG_DL' END"
                            );

                            // Convert carbohydrate units to enum names
                            database.execSQL(
                                    "UPDATE user_settings " +
                                            "SET defaultCarbUnit = CASE defaultCarbUnit " +
                                            "WHEN 'g KH' THEN 'GRAMS' " +
                                            "WHEN 'KE' THEN 'KE' " +
                                            "WHEN 'BE' THEN 'BE' " +
                                            "ELSE 'GRAMS' END"
                            );

                            // Convert Time of Day to enum names
                            database.execSQL(
                                    "UPDATE calculation_logs " +
                                            "SET timeOfDay = CASE timeOfDay " +
                                            "WHEN 'Morgens' THEN 'MORNING' " +
                                            "WHEN 'Mittags' THEN 'NOON' " +
                                            "WHEN 'Abend' THEN 'EVENING' " +
                                            "WHEN 'Nacht' THEN 'NIGHT' " +
                                            "ELSE 'MORNING' END"
                            );

                            // Convert carbohydrate units to enum names
                            database.execSQL(
                                    "UPDATE calculation_logs " +
                                            "SET carbUnit = CASE carbUnit " +
                                            "WHEN 'g KH' THEN 'GRAMS' " +
                                            "WHEN 'KE' THEN 'KE' " +
                                            "WHEN 'BE' THEN 'BE' " +
                                            "ELSE 'GRAMS' END"
                            );

                            database.execSQL("UPDATE user_settings " +
                                    "SET selectedAiModel = CASE selectedAiModel " +
                                    "WHEN 'gemini.3.5' THEN 'GEMINI_FLASH_3_5' " +
                                    "WHEN 'gemini-flash-lite-latest' THEN 'GEMINI_FLASH_LITE_LATEST' " +
                                    "ELSE 'GEMINI_FLASH_LITE_LATEST' END"
                            );
                        }
                    };

                    INSTANCE = Room.databaseBuilder(
                            appContext,
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                            .openHelperFactory(supportFactory)
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration(false)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
