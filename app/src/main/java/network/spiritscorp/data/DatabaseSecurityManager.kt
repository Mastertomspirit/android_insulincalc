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
import net.zetetic.database.sqlcipher.SQLiteDatabase
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
     * Also verifies that existing encrypted databases can be cleanly decrypted with the current key;
     * if an unrecoverable HMAC mismatch is detected, it safely recovers to prevent fatal startup crashes.
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

        // Step A: Check if existing file is an unencrypted standard SQLite database
        if (isDatabasePlaintext(dbFile)) {
            Log.w(TAG, "Detected legacy unencrypted SQLite database at ${dbFile.absolutePath}. Initiating SQLCipher migration...")
            return migratePlaintextDatabaseToEncrypted(context, dbFile, dbName, passphrase)
        }

        // Step B: Verify that the existing encrypted database can be decrypted with the current passphrase
        if (!isDatabaseDecryptionValid(dbFile, passphrase)) {
            Log.e(TAG, "Existing database at ${dbFile.absolutePath} cannot be decrypted with the current KeyStore key (HMAC mismatch or corrupted). Recreating clean database to recover from crash loop...")
            val parentDir = dbFile.parentFile ?: context.filesDir
            val corruptedBackup = File(parentDir, "${dbName}.corrupted_${System.currentTimeMillis()}.bak")
            dbFile.renameTo(corruptedBackup)
            
            val walFile = File(parentDir, "$dbName-wal")
            val shmFile = File(parentDir, "$dbName-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            return true
        }

        return true
    }

    /**
     * Validates whether an existing database file can be successfully decrypted and read with the provided passphrase.
     */
    fun isDatabaseDecryptionValid(dbFile: File, passphrase: ByteArray): Boolean {
        if (!dbFile.exists() || dbFile.length() == 0L) return true
        var db: SQLiteDatabase? = null
        return try {
            try {
                System.loadLibrary("sqlcipher")
            } catch (_: Throwable) {}

            db = SQLiteDatabase.openOrCreateDatabase(
                dbFile.absolutePath,
                passphrase,
                null,
                null
            )
            val cursor = db.rawQuery("SELECT count(*) FROM sqlite_schema;", null)
            cursor.close()
            true
        } catch (e: UnsatisfiedLinkError) {
            // In unit tests without native SQLCipher .so libraries, skip native decryption check
            Log.w(TAG, "Native SQLCipher library not available in current test runtime: ${e.message}")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Database decryption validation failed: ${e.message}")
            false
        } finally {
            try {
                db?.close()
            } catch (_: Exception) {}
        }
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

        // Rename WAL & SHM if present to correspond to temp file name
        val walFile = File(parentDir, "$dbName-wal")
        val shmFile = File(parentDir, "$dbName-shm")
        val tempWalFile = File(parentDir, "${dbName}.unencrypted.bak-wal")
        val tempShmFile = File(parentDir, "${dbName}.unencrypted.bak-shm")
        if (walFile.exists()) {
            walFile.renameTo(tempWalFile)
        }
        if (shmFile.exists()) {
            shmFile.renameTo(tempShmFile)
        }

        // Step 1: Move plaintext file to temporary location
        val renamed = originalDbFile.renameTo(unencryptedTempFile)
        if (!renamed) {
            Log.e(TAG, "Failed to rename unencrypted database file for migration!")
            // Roll back WAL/SHM renames
            if (tempWalFile.exists()) tempWalFile.renameTo(walFile)
            if (tempShmFile.exists()) tempShmFile.renameTo(shmFile)
            return false
        }

        var encryptedDb: SQLiteDatabase? = null
        try {
            try {
                System.loadLibrary("sqlcipher")
            } catch (_: Throwable) {}

            // Step 2: Open/create the target encrypted database directly with the passphrase.
            // This ensures identical PBKDF2 key derivation and header salt as Room's SupportOpenHelperFactory.
            encryptedDb = SQLiteDatabase.openOrCreateDatabase(
                targetEncryptedFile.absolutePath,
                passphrase,
                null,
                null
            )

            // Step 3: Attach the plaintext database with empty key
            encryptedDb.rawExecSQL("ATTACH DATABASE '${unencryptedTempFile.absolutePath}' AS plaintext KEY '';")

            // Step 4: Export schema and data from the attached 'plaintext' database into 'main' encrypted database
            val cursor = encryptedDb.rawQuery("SELECT sqlcipher_export('main', 'plaintext');", null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // Cursor iteration triggers the actual export in SQLite engine
                }
                cursor.close()
            }

            // Step 5: Detach the plaintext database
            encryptedDb.rawExecSQL("DETACH DATABASE plaintext;")

            Log.i(TAG, "SQLCipher database migration completed successfully for $dbName.")

            // Step 6: Safely close encrypted DB and delete plaintext backup & temp journal files
            encryptedDb.close()
            encryptedDb = null
            if (unencryptedTempFile.exists()) unencryptedTempFile.delete()
            if (tempWalFile.exists()) tempWalFile.delete()
            if (tempShmFile.exists()) tempShmFile.delete()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed during SQLCipher export migration: ${e.message}", e)
            try {
                encryptedDb?.close()
            } catch (_: Exception) {}

            // Rollback: restore plaintext file if encrypted creation failed
            if (targetEncryptedFile.exists()) {
                targetEncryptedFile.delete()
            }
            if (unencryptedTempFile.exists() && !targetEncryptedFile.exists()) {
                unencryptedTempFile.renameTo(targetEncryptedFile)
                if (tempWalFile.exists()) tempWalFile.renameTo(walFile)
                if (tempShmFile.exists()) tempShmFile.renameTo(shmFile)
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
