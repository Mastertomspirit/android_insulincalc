package network.spiritscorp.util;

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

/**
 * Central repository for system-wide schema versions, format identifiers,
 * storage constants, and protocol definitions.
 */
public final class AppConstants {

    private AppConstants() {
        // Utility / Constants class - prevent instantiation
    }

    // =========================================================================
    // Schema & Protocol Versions
    // =========================================================================

    /** Room SQLite Database Schema Version. */
    public static final int DATABASE_VERSION = 3;

    /** JSON Backup Payload Format Version. */
    public static final int JSON_BACKUP_VERSION = 3;

    /** CSV Export & Import Format Version. */
    public static final int CSV_BACKUP_VERSION = 2;

    /** Android KeyStore Encryption Key Vault Version. */
    public static final int SECURITY_KEY_VERSION = 3;


    // =========================================================================
    // Storage & Database Identifiers
    // =========================================================================

    /** Primary encrypted SQLite database filename. */
    public static final String DATABASE_NAME = "insulin_calculator.db";

    /**
     * Database passphrase key alias for password-based encryption.
     * This alias is used to store the password-derived database passphrase in Android KeyStore.
     */
    public static final String DB_PASSPHRASE_KEY_ALIAS = "network.spiritscorp.insulincalc.db_passphrase_key";

    /** Legacy KeyStore master encryption key alias. */
    public static final String LEGACY_KEY_ALIAS = "network.spiritscorp.insulincalc.db_vault_master_key_v2";

    /** Storage Keys for legacy mode */
    public static final String KEY_ENCRYPTED_PASSPHRASE = "secure_db_encrypted_passphrase";
    public static final String KEY_PASSPHRASE_IV = "secure_db_passphrase_iv";

    /** SharedPreferences file name for migration and preference flags. */
    public static final String SECURITY_PREFS_FILE = "secure_db_vault_prefs";

    /** Minimum password length for database encryption. */
    public static final int MIN_PASSWORD_LENGTH = 20;

    /** Number of PBKDF2 iterations for password-based key derivation. */
    public static final int PBKDF2_ITERATIONS = 100000;

    public static final int PASSPHRASE_BYTE_LENGTH = 32; // 256-bit AES Key

    public static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    /** Migration flag */
    public static final String KEY_MIGRATION_V3_COMPLETED = "migration_v3_completed";

    public static final int SALT_LENGTH = 32;

    // =========================================================================
    // Preference & Timeout Constants
    // =========================================================================

    /** Time duration in milliseconds before the medical disclaimer is shown again (30 days). */
    public static final long DISCLAIMER_RE_SHOW_INTERVAL_MS = 30L * 24 * 60 * 60 * 1000;
}
