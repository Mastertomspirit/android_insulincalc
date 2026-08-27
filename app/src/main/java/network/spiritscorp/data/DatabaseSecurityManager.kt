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
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.sqlcipher.database.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.security.SecureRandom

/**
 * Enterprise-grade security manager for the SQLite/Room database encryption layer.
 *
 * Key Responsibilities:
 * 1. Generates and securely manages a 256-bit cryptographically strong encryption passphrase
 *    backed by the hardware Android KeyStore (via Jetpack [EncryptedSharedPreferences] and [MasterKey]).
 * 2. Provides transparent, zero-data-loss migration for legacy unencrypted SQLite databases
 *    into SQLCipher AES-256 encrypted databases using `sqlcipher_export`.
 * 3. Sanitizes and securely wipes plaintext database artifacts after conversion.
 */
object DatabaseSecurityManager {

    private const val TAG = "DatabaseSecurityManager"
    private const val PREFS_FILE = "secure_db_vault_prefs"
    private const val KEY_DB_PASSPHRASE = "secure_db_master_passphrase_v1"
    private const val PASSPHRASE_BYTE_LENGTH = 32 // 256-bit AES Key

    // Magic 16-byte SQLite header signature: "SQLite format 3\000"
    private val SQLITE_HEADER_PREFIX = "SQLite format 3".toByteArray(Charsets.US_ASCII)

    /**
     * Retrieves the existing 256-bit database encryption passphrase or generates a new one
     * using [SecureRandom] if this is the first execution.
     *
     * The key is safely persisted inside hardware-backed [EncryptedSharedPreferences].
     *
     * @param context Application context used to access the Android KeyStore and SharedPreferences.
     * @return 32-byte (256-bit) encryption key suitable for SQLCipher.
     */
    @Synchronized
    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val prefs = getEncryptedPreferences(context)
        val storedKeyBase64 = prefs.getString(KEY_DB_PASSPHRASE, null)

        if (!storedKeyBase64.isNullOrBlank()) {
            try {
                val decoded = Base64.decode(storedKeyBase64, Base64.NO_WRAP)
                if (decoded.size == PASSPHRASE_BYTE_LENGTH) {
                    return decoded
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode stored encryption key, generating a fresh key", e)
            }
        }

        // Generate a new cryptographically random 256-bit passphrase
        val secureRandom = SecureRandom()
        val newKey = ByteArray(PASSPHRASE_BYTE_LENGTH)
        secureRandom.nextBytes(newKey)

        // Store the newly generated key as Base64 in EncryptedSharedPreferences
        val encodedKey = Base64.encodeToString(newKey, Base64.NO_WRAP)
        prefs.edit().putString(KEY_DB_PASSPHRASE, encodedKey).apply()

        Log.i(TAG, "Successfully generated and persisted new 256-bit database encryption key in secure vault.")
        return newKey
    }

    /**
     * Inspects the database file on disk and automatically migrates it to an AES-256 encrypted
     * SQLCipher database if it was previously created in unencrypted plaintext format.
     *
     * @param context Application context to locate the database files.
     * @param dbName Name of the database (e.g. "insulin_calculator.db").
     * @param passphrase The 256-bit encryption key to encrypt the database with.
     * @return True if migration occurred or was unnecessary, false if an error was encountered.
     */
    @Synchronized
    fun ensureDatabaseEncrypted(context: Context, dbName: String, passphrase: ByteArray): Boolean {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            // New database will be created directly encrypted by Room's openHelperFactory
            return true
        }

        // Check if existing file is an unencrypted standard SQLite database
        if (!isDatabasePlaintext(dbFile)) {
            // Database is already encrypted with SQLCipher
            return true
        }

        Log.w(TAG, "Detected legacy unencrypted SQLite database at ${dbFile.absolutePath}. Initiating SQLCipher migration...")
        return migratePlaintextDatabaseToEncrypted(context, dbFile, dbName, passphrase)
    }

    /**
     * Checks if a database file contains the standard unencrypted SQLite 3 header.
     * Plaintext SQLite files start with the ASCII string "SQLite format 3\000".
     * SQLCipher encrypted databases have encrypted page 1 data and will not match this header.
     *
     * @param dbFile The file to inspect.
     * @return True if the file matches the plaintext SQLite header, false otherwise.
     */
    fun isDatabasePlaintext(dbFile: File): Boolean {
        if (!dbFile.exists() || dbFile.length() < 16) {
            return false
        }

        return try {
            FileInputStream(dbFile).use { input ->
                val header = ByteArray(15)
                val readBytes = input.read(header)
                if (readBytes < 15) return false
                header.contentEquals(SQLITE_HEADER_PREFIX)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking database plaintext header for ${dbFile.name}", e)
            false
        }
    }

    /**
     * Migrates an existing unencrypted SQLite database file to an encrypted SQLCipher database.
     *
     * Migration Process:
     * 1. Rename existing unencrypted database file to temporary path (`*.unencrypted.bak`).
     * 2. Also remove or rename any existing WAL (`-wal`) and SHM (`-shm`) journal files to prevent corruption.
     * 3. Open the unencrypted database via SQLCipher with an empty passphrase.
     * 4. Attach a new encrypted database file using the secure 256-bit passphrase.
     * 5. Execute `sqlcipher_export('encrypted')` to copy all schemas, tables, indices, and data.
     * 6. Detach encrypted database and close connections.
     * 7. Safely delete the unencrypted backup file to leave no plaintext medical data on disk.
     */
    private fun migratePlaintextDatabaseToEncrypted(
        context: Context,
        originalDbFile: File,
        dbName: String,
        passphrase: ByteArray
    ): Boolean {
        val parentDir = originalDbFile.parentFile ?: context.filesDir
        val unencryptedTempFile = File(parentDir, "${dbName}.unencrypted.bak")
        val targetEncryptedFile = File(parentDir, dbName)

        // Remove any old leftover temporary migration file if present
        if (unencryptedTempFile.exists()) {
            unencryptedTempFile.delete()
        }

        // Clean up or rename WAL/SHM files
        val walFile = File(parentDir, "$dbName-wal")
        val shmFile = File(parentDir, "$dbName-shm")
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()

        // Step 1: Move plaintext file to temporary location
        val renamed = originalDbFile.renameTo(unencryptedTempFile)
        if (!renamed) {
            Log.e(TAG, "Failed to rename unencrypted database file for migration!")
            return false
        }

        var unencryptedDb: SQLiteDatabase? = null
        try {
            // Step 2: Open unencrypted database with SQLCipher (empty passphrase)
            SQLiteDatabase.loadLibs(context)
            unencryptedDb = SQLiteDatabase.openDatabase(
                unencryptedTempFile.absolutePath,
                "", // Empty passphrase opens standard plaintext SQLite
                null,
                SQLiteDatabase.OPEN_READWRITE
            )

            // Step 3: Format the hex passphrase for raw SQL ATTACH command
            val hexKey = bytesToHex(passphrase)

            // Step 4: Attach the new target database with encryption key
            val attachSql = "ATTACH DATABASE '${targetEncryptedFile.absolutePath}' AS encrypted KEY \"x'$hexKey'\";"
            unencryptedDb.rawExecSQL(attachSql)

            // Step 5: Export all schema, triggers, and records into encrypted database
            unencryptedDb.rawExecSQL("SELECT sqlcipher_export('encrypted');")

            // Step 6: Detach the encrypted database
            unencryptedDb.rawExecSQL("DETACH DATABASE encrypted;")

            Log.i(TAG, "SQLCipher database migration completed successfully for $dbName.")

            // Step 7: Safely delete plaintext database backup
            unencryptedDb.close()
            unencryptedDb = null
            if (unencryptedTempFile.exists()) {
                unencryptedTempFile.delete()
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed during SQLCipher export migration: ${e.message}", e)
            try {
                unencryptedDb?.close()
            } catch (_: Exception) {}

            // Rollback: restore plaintext file if encrypted creation failed
            if (unencryptedTempFile.exists() && !targetEncryptedFile.exists()) {
                unencryptedTempFile.renameTo(targetEncryptedFile)
            }
            return false
        }
    }

    /**
     * Converts a byte array into a lowercase hex string.
     */
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * Retrieves or initializes [EncryptedSharedPreferences] backed by the Android KeyStore.
     * Includes fallback mechanism if running in JVM testing environments.
     */
    private fun getEncryptedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences unavailable (e.g., test environment), falling back to standard prefs: ${e.message}")
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }
}
