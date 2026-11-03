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
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import network.spiritscorp.util.AppConstants;

/**
 * Enterprise-grade security manager for SQLite / SQLCipher database encryption.
 * Supports both legacy KeyStore-based encryption and new password-based encryption.
 *
 * <p>Hybrid approach:</p>
 * <ul>
 *   <li>Legacy mode: Uses KeyStore-generated passphrase (backward compatible)</li>
 *   <li>Password mode: Uses password-derived passphrase (cross-device recovery)</li>
 * </ul>
 *
 * <p>Priority order for passphrase retrieval:</p>
 * <ol>
 *   <li>Password-based passphrase (if password is set)</li>
 *   <li>Legacy KeyStore passphrase</li>
 *   <li>Generate new passphrase</li>
 * </ol>
 */
public class DatabaseSecurityManager {

    /**
     * Logging tag used for debugging and security audits.
     */
    private static final String TAG = "DatabaseSecurityManager";
    private static final String PREFS_FILE = AppConstants.SECURITY_PREFS_FILE;

    // Android KeyStore Alias & Cipher specifications
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final int KEY_VERSION = AppConstants.SECURITY_KEY_VERSION;
    private static final String LEGACY_KEY_ALIAS = AppConstants.SECURITY_KEY_ALIAS;
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int PASSPHRASE_BYTE_LENGTH = 32; // 256-bit AES Key

    // Storage Keys for legacy mode
    private static final String KEY_ENCRYPTED_PASSPHRASE = "secure_db_encrypted_passphrase";
    private static final String KEY_PASSPHRASE_IV = "secure_db_passphrase_iv";
    private static final String FALLBACK_KEY_PASSPHRASE = "secure_db_passphrase_fallback";

    // Migration flag
    private static final String KEY_MIGRATION_V3_COMPLETED = "migration_v3_completed";

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private final SharedPreferences mPrefs;
    private final PasswordDerivationManager mPasswordManager;

    /**
     * Constructs a new DatabaseSecurityManager instance bound to the provided Android context.
     *
     * @param context Application or activity context
     */
    public DatabaseSecurityManager(@NonNull Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE));
    }

    /**
     * Constructs a new DatabaseSecurityManager instance with custom SharedPreferences,
     * specifically useful for dependency injection and isolated unit testing.
     *
     * @param prefs SharedPreferences instance to store encrypted key data
     */
    public DatabaseSecurityManager(@NonNull SharedPreferences prefs) {
        this.mPrefs = prefs;
        this.mPasswordManager = new PasswordDerivationManager();
    }

    /**
     * Retrieves the existing 256-bit database encryption passphrase.
     * Priority: 1) Password-based, 2) Legacy KeyStore, 3) Generate new.
     *
     * @return 32-byte (256-bit) encryption key suitable for SQLCipher
     */
    @NonNull
    public synchronized byte[] getOrCreateDatabasePassphrase() {
        // 1. Try password-based passphrase first
        byte[] passwordPassphrase = getPasswordBasedPassphrase();
        if (passwordPassphrase != null) {
            return passwordPassphrase;
        }

        // 2. Try legacy KeyStore-encrypted passphrase
        byte[] legacyPassphrase = getLegacyPassphrase();
        if (legacyPassphrase != null) {
            return legacyPassphrase;
        }

        // 3. Generate a new cryptographically random 256-bit passphrase
        SecureRandom secureRandom = new SecureRandom();
        byte[] newKey = new byte[PASSPHRASE_BYTE_LENGTH];
        secureRandom.nextBytes(newKey);

        saveLegacyPassphrase(newKey);
        return newKey;
    }

    /**
     * Retrieves password-based passphrase if password is set.
     *
     * @return 32-byte passphrase or null if password not set
     */
    @Nullable
    private byte[] getPasswordBasedPassphrase() {
        if (mPasswordManager.isPasswordSet()) {
            return mPasswordManager.loadFromKeyStore();
        }
        return null;
    }

    /**
     * Retrieves legacy KeyStore-encrypted passphrase.
     *
     * @return 32-byte passphrase or null if not found
     */
    @Nullable
    private byte[] getLegacyPassphrase() {
        String encryptedPassphraseBase64 = mPrefs.getString(KEY_ENCRYPTED_PASSPHRASE, null);
        String ivBase64 = mPrefs.getString(KEY_PASSPHRASE_IV, null);

        if (encryptedPassphraseBase64 != null && !encryptedPassphraseBase64.trim().isEmpty() &&
                ivBase64 != null && !ivBase64.trim().isEmpty()) {
            try {
                byte[] encryptedBytes = Base64.decode(encryptedPassphraseBase64, Base64.NO_WRAP);
                byte[] ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP);
                byte[] decrypted = decryptWithKeyStore(ivBytes, encryptedBytes);
                if (decrypted != null && decrypted.length == PASSPHRASE_BYTE_LENGTH) {
                    return decrypted;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt stored KeyStore passphrase: " + e.getMessage(), e);
            }
        }

        // Check fallback storage (used in JVM test environments)
        String fallbackBase64 = mPrefs.getString(FALLBACK_KEY_PASSPHRASE, null);
        if (fallbackBase64 != null && !fallbackBase64.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(fallbackBase64, Base64.NO_WRAP);
                if (decoded != null && decoded.length == PASSPHRASE_BYTE_LENGTH) {
                    return decoded;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode fallback passphrase: " + e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * Saves passphrase using legacy KeyStore encryption.
     *
     * @param rawPassphrase Passphrase to save
     */
    private void saveLegacyPassphrase(@NonNull byte[] rawPassphrase) {
        EncryptedData encryptedData = encryptWithKeyStore(rawPassphrase);
        if (encryptedData != null) {
            mPrefs.edit()
                    .putString(KEY_PASSPHRASE_IV, Base64.encodeToString(encryptedData.iv(), Base64.NO_WRAP))
                    .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedData.ciphertext(), Base64.NO_WRAP))
                    .apply();
        } else {
            // Fallback for JVM test environments where KeyStore provider is absent
            mPrefs.edit()
                    .putString(FALLBACK_KEY_PASSPHRASE, Base64.encodeToString(rawPassphrase, Base64.NO_WRAP))
                    .apply();
        }
    }

    /**
     * Sets a password for database encryption.
     * Derives passphrase from password and stores it in KeyStore.
     *
     * @param userPassword User-provided password (must be >= MIN_PASSWORD_LENGTH)
     * @return true if successful, false otherwise
     * @throws IllegalArgumentException if password is too short
     */
    public boolean setPassword(@NonNull String userPassword) {
        return mPasswordManager.setPassword(userPassword);
    }

    /**
     * Verifies if a password is valid.
     *
     * @param userPassword Password to verify
     * @return true if password is valid, false otherwise
     */
    public boolean verifyPassword(@NonNull String userPassword) {
        return mPasswordManager.verifyPassword(userPassword);
    }

    /**
     * Changes the password and re-encrypts the database.
     *
     * @param oldPassword Current password
     * @param newPassword New password
     * @return true if successful, false otherwise
     * @throws SecurityException if old password is invalid
     * @throws IllegalArgumentException if passwords are invalid
     */
    public boolean changePassword(@NonNull String oldPassword, @NonNull String newPassword) {
        return mPasswordManager.changePassword(oldPassword, newPassword);
    }

    /**
     * Checks if a password is set.
     *
     * @return true if password is set, false otherwise
     */
    public boolean isPasswordSet() {
        return mPasswordManager.isPasswordSet();
    }

    /**
     * Generates a random password for new installations.
     *
     * @return Randomly generated password
     */
    @NonNull
    public String generateRandomPassword() {
        return mPasswordManager.generateRandomPassword();
    }

    /**
     * Gets the password derivation manager for advanced operations.
     *
     * @return PasswordDerivationManager instance
     */
    @NonNull
    public PasswordDerivationManager getPasswordManager() {
        return mPasswordManager;
    }

    /**
     * Checks if migration to v3 has been completed.
     *
     * @return true if migration completed, false otherwise
     */
    public boolean isMigrationV3Completed() {
        return mPrefs.getBoolean(KEY_MIGRATION_V3_COMPLETED, false);
    }

    /**
     * Marks migration to v3 as completed.
     */
    public void markMigrationV3Completed() {
        mPrefs.edit().putBoolean(KEY_MIGRATION_V3_COMPLETED, true).apply();
    }

    /**
     * Derives a database passphrase from a password without storing it.
     * Useful for testing or when you need the passphrase but don't want to persist it.
     *
     * @param password User password
     * @return 32-byte passphrase or null if derivation fails
     * @throws IllegalArgumentException if password is too short
     */
    @Nullable
    public byte[] derivePassphraseFromPassword(@NonNull String password) {
        return mPasswordManager.deriveDbPassphrase(password);
    }

    /**
     * Gets the current database passphrase (from any source).
     * This is useful when you need the actual passphrase bytes for database operations.
     *
     * @return 32-byte passphrase or null if not available
     */
    @Nullable
    public byte[] getCurrentPassphrase() {
        byte[] passwordPassphrase = getPasswordBasedPassphrase();
        if (passwordPassphrase != null) {
            return passwordPassphrase;
        }
        return getLegacyPassphrase();
    }

    private SecretKey getOrCreateLegacyKeyStoreMasterKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (keyStore.containsAlias(LEGACY_KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(LEGACY_KEY_ALIAS, null);
            if (entry != null) {
                return entry.getSecretKey();
            }
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
        );
        KeyGenParameterSpec keyGenSpec = new KeyGenParameterSpec.Builder(
                LEGACY_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build();

        keyGenerator.init(keyGenSpec);
        return keyGenerator.generateKey();
    }

    /**
     * Encrypts the provided plaintext byte array using the Android KeyStore AES-GCM master key.
     *
     * @param plaintext Raw bytes to encrypt (e.g., database passphrase)
     * @return EncryptedData containing the initialization vector (IV) and ciphertext, or null if KeyStore is unavailable
     */
    @Nullable
    EncryptedData encryptWithKeyStore(@NonNull byte[] plaintext) {
        try {
            SecretKey secretKey = getOrCreateLegacyKeyStoreMasterKey();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext);
            return new EncryptedData(iv, ciphertext);
        } catch (Throwable e) {
            Log.w(TAG, "AndroidKeyStore encryption not available (e.g. JVM/test environment)");
            return null;
        }
    }

    /**
     * Decrypts the provided ciphertext byte array using the Android KeyStore AES-GCM master key and IV.
     *
     * @param iv Initialization vector used during encryption
     * @param ciphertext Encrypted data bytes
     * @return Decrypted raw plaintext bytes, or null if decryption fails or KeyStore is unavailable
     */
    @Nullable
    byte[] decryptWithKeyStore(@NonNull byte[] iv, @NonNull byte[] ciphertext) {
        try {
            SecretKey secretKey = getOrCreateLegacyKeyStoreMasterKey();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(ciphertext);
        } catch (Throwable e) {
            Log.w(TAG, "AndroidKeyStore decryption not available (e.g. JVM/test environment)");
            return null;
        }
    }

    /**
     * Converts a byte array into a lowercase hex string.
     *
     * @param bytes Raw byte array
     * @return Hexadecimal representation
     */
    @NonNull
    public static String bytesToHex(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Immutable container record holding encrypted ciphertext and its initialization vector.
     */
    public record EncryptedData(byte[] iv, byte[] ciphertext) {
    }
}
