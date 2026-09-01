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
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.UserSettings;

/**
 * Primary Room Database for the Insulin Calculator application written in Java.
 *
 * Security & Architecture Highlights:
 * - Backed by SQLCipher 256-bit AES database encryption.
 * - Hardware-backed passphrase generated and secured via Android KeyStore.
 * - Stores all patient calculation logs and user therapy configurations locally on device.
 */
@Database(
        entities = {CalculationLog.class, UserSettings.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "insulin_calculator.db";
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

                    INSTANCE = Room.databaseBuilder(
                            appContext,
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                            .openHelperFactory(supportFactory)
                            .fallbackToDestructiveMigration(false)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
