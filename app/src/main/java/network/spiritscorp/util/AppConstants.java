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

    /**
     * Room SQLite Database Schema Version.
     */
    public static final int DATABASE_VERSION = 1;

    /**
     * JSON Backup Payload Format Version.
     */
    public static final int JSON_BACKUP_VERSION = 1;

    /**
     * CSV Export & Import Format Version.
     */
    public static final int CSV_BACKUP_VERSION = 1;

    /**
     * Android KeyStore Encryption Key Vault Version.
     */
    public static final int SECURITY_KEY_VERSION = 2;

    // =========================================================================
    // Storage & Database Identifiers
    // =========================================================================

    /**
     * Primary encrypted SQLite database filename.
     */
    public static final String DATABASE_NAME = "insulin_calculator.db";

    /**
     * KeyStore master encryption key alias.
     */
    public static final String SECURITY_KEY_ALIAS = "network.spiritscorp.insulincalc.db_vault_master_key_v2";

    /**
     * SharedPreferences file name for cryptographic key metadata.
     */
    public static final String SECURITY_PREFS_FILE = "secure_db_vault_prefs";
}
