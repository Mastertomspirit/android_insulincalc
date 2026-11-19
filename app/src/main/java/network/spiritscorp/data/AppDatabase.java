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
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import java.util.Objects;

import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.UserSettings;
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

    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;

    /** Data access object for querying and persisting {@link CalculationLog} entries. */
    public abstract CalculationLogDao calculationLogDao();

    /** Data access object for managing personalized {@link UserSettings} therapy factors and preferences. */
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
                    try {
                        Context appContext = context.getApplicationContext();

                        // Initialize SQLCipher native binaries
                        try {
                            System.loadLibrary("sqlcipher");
                        } catch (UnsatisfiedLinkError | SecurityException |
                                 NullPointerException e) {
                            throw new IllegalStateException("Failed to load SQLCipher native library", e);
                        }

                        SupportOpenHelperFactory supportFactory = getSupportFactory(appContext);

                        INSTANCE = Room.databaseBuilder(
                                        appContext,
                                        AppDatabase.class,
                                        DatabaseConstants.DATABASE_NAME
                                )
                                .openHelperFactory(supportFactory)
                                .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                                .addMigrations(DatabaseMigrations.MIGRATION_2_3)
                                .fallbackToDestructiveMigration(false)
                                .allowMainThreadQueries()
                                .build();

                    }catch(IllegalStateException e){
                        Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                        return null;
                    }
                }
            }
        }
        return INSTANCE;
    }

    /** Resets the AppDatabase singleton instance, closes active database connections and deletes the existing database file. */
    static void resetInstance() {
        if (AppDatabase.INSTANCE != null) {
            if (AppDatabase.INSTANCE.isOpen())  AppDatabase.INSTANCE.close();
            AppDatabase.INSTANCE = null;
        }
    }

    /**
     * Retrieves or generates a hardware-secured 256-bit encryption key
     * and configures Room directly with the SQLCipher SupportOpenHelperFactory.
     * <p>
     * 1. Retrieves a hardware-backed 256-bit encryption key from the
     *    Android KeyStore, or generates a new one if it doesn't exist yet.
     * 2. Configures and returns a SQLCipher {@link SupportOpenHelperFactory}
     *    to ensure encrypted local database storage for Room.
     *
     * @param appContext The application context used to access the KeyStore and database.
     * @return A configured {@link SupportOpenHelperFactory} using the encrypted database key.
     */
    private static SupportOpenHelperFactory getSupportFactory(Context appContext) {
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(appContext);
        SupportOpenHelperFactory supportFactory;
        try {
            byte[] passphrase = securityManager.getOrCreateDatabasePassphrase();
            supportFactory = new SupportOpenHelperFactory(passphrase);
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Failed to retrieve or generate database encryption passphrase: " + e.getMessage(), e);
        }
        return supportFactory;
    }
}
