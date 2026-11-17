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
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import static network.spiritscorp.util.AppConstants.*;

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

    // Android KeyStore Alias & Cipher specifications
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SharedPreferences mPrefs;
    private final PasswordDerivationManager mPasswordManager;

    /**
     * Immutable container record holding encrypted ciphertext and its initialization vector.
     */
    record EncryptedData(byte[] iv, byte[] ciphertext) {
    }

    /**
     * Constructs a new DatabaseSecurityManager instance bound to the provided Android context.
     *
     * @param context Application or activity context
     */
    public DatabaseSecurityManager(@NonNull Context context) {
        this(context.getApplicationContext().getSharedPreferences(SECURITY_PREFS_FILE, Context.MODE_PRIVATE));
    }

    /**
     * Constructs a new DatabaseSecurityManager instance with custom SharedPreferences,
     * specifically useful for dependency injection and isolated unit testing.
     *
     * @param prefs SharedPreferences instance to store encrypted key data
     */
    DatabaseSecurityManager(@NonNull SharedPreferences prefs) {
        this.mPrefs = prefs;
        this.mPasswordManager = new PasswordDerivationManager();
    }

    /**
     * Retrieves the existing 256-bit database encryption passphrase.
     * Priority: 1) Password-based, 2) Legacy KeyStore.
     *
     * @return 32-byte (256-bit) encryption key suitable for SQLCipher
     */
    @Nullable
    public byte[] getDatabasePassphrase() {
        // 1. Try password-based passphrase first
        byte[] passwordPassphrase = mPasswordManager.loadFromKeyStore();
        if (passwordPassphrase != null) {
            return passwordPassphrase;
        }

        // 2. Try legacy KeyStore-encrypted passphrase
        return getLegacyPassphrase();
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
                EncryptedData entry = new EncryptedData(
                        Base64.decode(encryptedPassphraseBase64.trim(), Base64.NO_WRAP),
                        Base64.decode(ivBase64.trim(), Base64.NO_WRAP)
                );
                byte[] decrypted = decryptWithKeyStore(entry);
                if (decrypted != null && decrypted.length == PASSPHRASE_BYTE_LENGTH) {
                    return decrypted;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt stored KeyStore passphrase: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Sets a password for database encryption.
     * Derives passphrase from password and stores it in KeyStore.
     *
     * @param userPassword User-provided password (must be >= MIN_PASSWORD_LENGTH)
     * @return true if successful, false otherwise
     * @throws IllegalArgumentException if password is too short
     */
    boolean setPassword(@NonNull String userPassword) {
        if (!mPasswordManager.isPasswordSet()) {
            return mPasswordManager.setPassword(userPassword);
        }
        return false;
    }

    /**
     * Verifies if a password is valid.
     *
     * @param userPassword Password to verify
     * @return true if password is valid, false otherwise
     */
    boolean verifyPassword(@NonNull String userPassword) {
        if (mPasswordManager.isPasswordSet()) {
            return mPasswordManager.verifyPassword(userPassword);
        }
        return false;
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
        if (!mPasswordManager.isPasswordSet()) {
            return setPassword(newPassword);
        }else {
            return mPasswordManager.changePassword(oldPassword, newPassword);
        }
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
     * Generates a random password for UI pre-filling or initial setup.
     *
     * @return Randomly generated password
     */
    @NonNull
    public String generateRandomPassword() {
        return mPasswordManager.generateRandomPassword();
    }

    /**
     * Checks if legacy KeyStore passphrase exists.
     *
     * @return true if legacy passphrase exists, false otherwise
     */
    public boolean hasLegacyPassphrase() {
        String encryptedPassphraseBase64 = mPrefs.getString(KEY_ENCRYPTED_PASSPHRASE, null);
        String ivBase64 = mPrefs.getString(KEY_PASSPHRASE_IV, null);
        return encryptedPassphraseBase64 != null && !encryptedPassphraseBase64.trim().isEmpty() &&
                ivBase64 != null && !ivBase64.trim().isEmpty();
    }

    /**
     * Checks if migration to v3 has been completed.
     * Sets migration flag to false if legacy passphrase is set.
     *
     * @return true if migration completed, false otherwise
     */
    public boolean isMigrationV3Completed() {
        if (hasLegacyPassphrase()) {
            if (mPrefs.getBoolean(KEY_MIGRATION_V3_COMPLETED, true)) {
                mPrefs.edit().putBoolean(KEY_MIGRATION_V3_COMPLETED, false).apply();
            }
            return false;
        }
        return mPrefs.getBoolean(KEY_MIGRATION_V3_COMPLETED, false);
    }

    /**
     * Marks migration to v3 as completed.
     */
    public void markMigrationV3Completed() {
        mPrefs.edit().putBoolean(KEY_MIGRATION_V3_COMPLETED, true).apply();
    }

    @Nullable
    private SecretKey getLegacyKeyStoreMasterKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(LEGACY_KEY_ALIAS)) {
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(LEGACY_KEY_ALIAS, null);
                if (entry != null) {
                    return entry.getSecretKey();
                }
            }
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | KeyStoreException e) {
            Log.e(TAG, "Failed to load passphrase from KeyStore: " + e.getMessage(), e);
        } catch (CertificateException | IOException e) {
            Log.e(TAG, "No Keystore found: " + e.getMessage(), e);
        }
        return null;
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
            SecretKey secretKey = getLegacyKeyStoreMasterKey();
            if (secretKey == null) {
                return null;
            }
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext);
            return new EncryptedData(iv, ciphertext);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "AndroidKeyStore encryption not available (e.g. JVM/test environment)");
            return null;
        } catch (InvalidKeyException e) {
            Log.e(TAG, "The key is not valid");
            return null;
        }
    }

    /**
     * Decrypts the provided ciphertext byte array using the Android KeyStore AES-GCM master key and IV.
     *
     * @param data Record with initialization vector used during encryption and encrypted data bytes
     * @return Decrypted raw plaintext bytes, or null if decryption fails or KeyStore is unavailable
     */
    @Nullable
    byte[] decryptWithKeyStore(@NonNull EncryptedData data) {
        try {
            SecretKey secretKey = getLegacyKeyStoreMasterKey();
            if (secretKey == null) {
                return null;
            }
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, data.iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(data.ciphertext);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.e(TAG, "AndroidKeyStore decryption not available (e.g. JVM/test environment)");
            return null;
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            Log.e(TAG, "The key is not valid");
            return null;
        }
    }
}
