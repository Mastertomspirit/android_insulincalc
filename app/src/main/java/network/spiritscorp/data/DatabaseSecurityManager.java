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
 * Enterprise-grade security manager object for SQLite / SQLCipher database encryption.
 *<br><br>
 * Supports two encryption modes:<br>
 * 1. Legacy: Hardware-backed Android KeyStore with random passphrase (for existing users).<br>
 * 2. Password-based: Deterministic passphrase derived from user password, stored in KeyStore.<br>
 *    This enables database recovery on new devices using the same password.<br>
 *<br>
 * Designed as an instantiable, object-oriented cryptographic vault:<br>
 * - Hardware-backed Android KeyStore protection for legacy users.<br>
 * - Password-based deterministic derivation for new users and recovery.<br>
 * - JVM / Test Resilience: Gracefully falls back in local JVM unit testing environments.
 */
public class DatabaseSecurityManager {

    /**
     * Logging tag used for debugging and security audits.
     */
    private static final String TAG = "DatabaseSecurityManager";

    // Legacy KeyStore constants (for existing users)
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String LEGACY_KEY_ALIAS = AppConstants.SECURITY_KEY_ALIAS;
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = AppConstants.GCM_TAG_LENGTH_BITS;
    private static final int PASSPHRASE_BYTE_LENGTH = AppConstants.PASSPHRASE_BYTE_LENGTH;

    // Storage Keys for legacy passphrase encryption
    private static final String KEY_ENCRYPTED_PASSPHRASE = "secure_db_encrypted_passphrase";
    private static final String KEY_PASSPHRASE_IV = "secure_db_passphrase_iv";
    private static final String FALLBACK_KEY_PASSPHRASE = "secure_db_passphrase_fallback";

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private final SharedPreferences mPrefs;
    private final PasswordDerivationManager mPasswordManager;

    /**
     * Constructs a new DatabaseSecurityManager instance bound to the provided Android context.
     *
     * @param context Application or activity context.
     */
    public DatabaseSecurityManager(@NonNull Context context) {
        this.mPrefs = context.getApplicationContext()
                .getSharedPreferences(AppConstants.SECURITY_PREFS_FILE, Context.MODE_PRIVATE);
        this.mPasswordManager = new PasswordDerivationManager();
    }

    /**
     * Constructs a new DatabaseSecurityManager instance with custom SharedPreferences,
     * specifically useful for dependency injection and isolated unit testing.
     *
     * @param prefs SharedPreferences instance to store encrypted key data.
     */
    public DatabaseSecurityManager(@NonNull SharedPreferences prefs) {
        this.mPrefs = prefs;
        this.mPasswordManager = new PasswordDerivationManager();
    }

    // =========================================================================
    // Main Passphrase Retrieval
    // =========================================================================

    /**
     * Retrieves the database encryption passphrase using hybrid loading:
     * 1. Try password-based derivation first (if password is set)
     * 2. Fall back to legacy KeyStore (for existing users)
     * 3. Generate new random passphrase if neither is available
     *
     * @return 32-byte (256-bit) encryption key suitable for SQLCipher.
     */
    @NonNull
    public synchronized byte[] getOrCreateDatabasePassphrase() {
        // 1. Try password-based derivation first
        byte[] passwordPassphrase = tryLoadPasswordBased();
        if (passwordPassphrase != null) {
            Log.i(TAG, "Loaded database passphrase from password derivation");
            return passwordPassphrase;
        }

        // 2. Fall back to legacy KeyStore
        byte[] legacyPassphrase = tryLoadLegacy();
        if (legacyPassphrase != null) {
            Log.i(TAG, "Loaded database passphrase from legacy KeyStore");
            return legacyPassphrase;
        }

        // 3. Generate new random passphrase (first run)
        return generateAndStoreNewPassphrase();
    }

    /**
     * Retrieves the database passphrase using password-based derivation.
     *
     * @return 32-byte passphrase, or null if not set.
     */
    @Nullable
    private byte[] tryLoadPasswordBased() {
        return mPasswordManager.loadFromKeyStore();
    }

    /**
     * Retrieves the database passphrase from legacy KeyStore.
     *
     * @return 32-byte passphrase, or null if not available.
     */
    @Nullable
    private byte[] tryLoadLegacy() {
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
                Log.e(TAG, "Failed to decrypt legacy passphrase: " + e.getMessage(), e);
            }
        }

        // Fallback for JVM test environments
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
     * Generates a new random database passphrase and stores it using legacy method.
     *
     * @return New 32-byte database passphrase.
     */
    @NonNull
    private byte[] generateAndStoreNewPassphrase() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] newKey = new byte[PASSPHRASE_BYTE_LENGTH];
        secureRandom.nextBytes(newKey);

        savePassphraseWithKeyStore(newKey);
        Log.i(TAG, "Successfully generated and persisted new 256-bit database encryption key");
        return newKey;
    }

    // =========================================================================
    // Password-Based Methods
    // =========================================================================

    /**
     * Sets a user password for database encryption.
     * Derives a deterministic passphrase and stores it in KeyStore.
     *
     * @param context Android context.
     * @param userPassword User-provided password (must be >= 20 characters).
     * @return true if password was set successfully, false otherwise.
     */
    public boolean setPassword(@NonNull Context context, @NonNull String userPassword) {
        if (userPassword.length() < AppConstants.MIN_PASSPHRASE_LENGTH) {
            Log.w(TAG, "Password too short. Minimum " + AppConstants.MIN_PASSPHRASE_LENGTH + " characters required");
            return false;
        }

        return mPasswordManager.setPassword(context, userPassword);
    }

    /**
     * Verifies if a password can derive the stored database passphrase.
     *
     * @param userPassword User-provided password to verify.
     * @return true if password is valid, false otherwise.
     */
    public boolean verifyPassword(@NonNull String userPassword) {
        if (userPassword.length() < AppConstants.MIN_PASSPHRASE_LENGTH) {
            return false;
        }
        return mPasswordManager.verifyPassword(userPassword);
    }

    /**
     * Clears the password-based key from KeyStore.
     *
     * @return true if password was cleared, false otherwise.
     */
    public boolean clearPassword() {
        return mPasswordManager.clearPassword();
    }

    /**
     * Checks if a password-based key exists in KeyStore.
     *
     * @return true if password is set, false otherwise.
     */
    public boolean isPasswordSet() {
        return mPasswordManager.isPasswordSet();
    }

    /**
     * Gets the database passphrase derived from password.
     * Used when KeyStore is unavailable (e.g., on new device).
     *
     * @param userPassword User-provided password.
     * @return 32-byte database passphrase, or null if derivation fails.
     */
    @Nullable
    public byte[] getPassphraseFromPassword(@NonNull String userPassword) {
        if (userPassword.length() < AppConstants.MIN_PASSPHRASE_LENGTH) {
            return null;
        }
        return mPasswordManager.deriveDbPassphrase(userPassword);
    }

    // =========================================================================
    // Legacy KeyStore Methods (for existing users)
    // =========================================================================

    /**
     * Saves the database passphrase encrypted with KeyStore master key.
     *
     * @param rawPassphrase Database passphrase to save.
     */
    private void savePassphraseWithKeyStore(byte[] rawPassphrase) {
        EncryptedData encryptedData = encryptWithKeyStore(rawPassphrase);
        if (encryptedData != null) {
            mPrefs.edit()
                    .putString(KEY_PASSPHRASE_IV, Base64.encodeToString(encryptedData.iv(), Base64.NO_WRAP))
                    .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedData.ciphertext(), Base64.NO_WRAP))
                    .apply();
        } else {
            // Fallback for JVM test environments
            mPrefs.edit()
                    .putString(FALLBACK_KEY_PASSPHRASE, Base64.encodeToString(rawPassphrase, Base64.NO_WRAP))
                    .apply();
        }
    }

    /**
     * Encrypts the provided plaintext byte array using the Android KeyStore AES-GCM master key.
     *
     * @param plaintext Raw bytes to encrypt (e.g., database passphrase).
     * @return EncryptedData containing the initialization vector (IV) and ciphertext, or null if KeyStore is unavailable.
     */
    @Nullable
    private EncryptedData encryptWithKeyStore(@NonNull byte[] plaintext) {
        try {
            SecretKey secretKey = getOrCreateKeyStoreMasterKey();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext);
            return new EncryptedData(iv, ciphertext);
        } catch (Throwable e) {
            Log.w(TAG, "AndroidKeyStore encryption not available (e.g. JVM/test environment): " + e.getMessage());
            return null;
        }
    }

    /**
     * Decrypts the provided ciphertext byte array using the Android KeyStore AES-GCM master key and IV.
     *
     * @param iv Initialization vector used during encryption.
     * @param ciphertext Encrypted data bytes.
     * @return Decrypted raw plaintext bytes, or null if decryption fails or KeyStore is unavailable.
     */
    @Nullable
    private byte[] decryptWithKeyStore(@NonNull byte[] iv, @NonNull byte[] ciphertext) {
        try {
            SecretKey secretKey = getOrCreateKeyStoreMasterKey();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(ciphertext);
        } catch (Throwable e) {
            Log.w(TAG, "AndroidKeyStore decryption not available (e.g. JVM/test environment): " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets or creates the Android KeyStore master key for legacy encryption.
     *
     * @return SecretKey for encryption/decryption.
     * @throws Exception if key generation fails.
     */
    private SecretKey getOrCreateKeyStoreMasterKey() throws Exception {
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

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Converts a byte array into a lowercase hex string.
     *
     * @param bytes Raw byte array.
     * @return Hexadecimal representation.
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
