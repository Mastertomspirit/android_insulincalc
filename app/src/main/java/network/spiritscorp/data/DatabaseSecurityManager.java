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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import network.spiritscorp.util.AppConstants;

/**
 * Manages database encryption passphrase with password-based fallback.
 * Always provides a passphrase - either from password derivation or legacy KeyStore.
 */
public class DatabaseSecurityManager {

    private static final String TAG = "DatabaseSecurityMgr";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String LEGACY_KEY_ALIAS = AppConstants.SECURITY_KEY_ALIAS;
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    // Storage keys for legacy encryption
    private static final String KEY_ENCRYPTED_PASSPHRASE = "secure_db_encrypted_passphrase";
    private static final String KEY_PASSPHRASE_IV = "secure_db_passphrase_iv";
    private static final String FALLBACK_KEY_PASSPHRASE = "secure_db_passphrase_fallback";

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private final SharedPreferences prefs;
    private final PasswordDerivationManager passwordManager;

    /**
     * Constructs DatabaseSecurityManager with application context.
     *
     * @param context Application context
     */
    public DatabaseSecurityManager(@NonNull Context context) {
        this(context.getApplicationContext()
                .getSharedPreferences(AppConstants.SECURITY_PREFS_FILE, Context.MODE_PRIVATE));
    }

    /**
     * Constructs DatabaseSecurityManager with custom SharedPreferences for testing.
     *
     * @param prefs SharedPreferences instance
     */
    public DatabaseSecurityManager(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
        this.passwordManager = new PasswordDerivationManager();
    }

    /**
     * Gets the database encryption passphrase.
     * Priority: 1) Password-based, 2) Legacy KeyStore, 3) Generate new.
     *
     * @return 32-byte passphrase, never null
     */
    @NonNull
    public byte[] getDatabasePassphrase() {
        // 1. Try password-based
        byte[] passwordPassphrase = passwordManager.loadFromKeyStore();
        if (passwordPassphrase != null) {
            return passwordPassphrase;
        }

        // 2. Try legacy
        byte[] legacyPassphrase = loadLegacyPassphrase();
        if (legacyPassphrase != null) {
            return legacyPassphrase;
        }

        // 3. Generate new
        return generateNewPassphrase();
    }

    /**
     * Gets passphrase derived from password without storing it.
     * Used for recovery when KeyStore is unavailable.
     *
     * @param password User password
     * @return 32-byte passphrase or null if derivation fails
     */
    @Nullable
    public byte[] derivePassphrase(@NonNull String password) {
        try {
            return passwordManager.deriveDbPassphrase(password);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Verifies if password matches the stored passphrase.
     *
     * @param password Password to verify
     * @return true if password matches
     */
    public boolean verifyPassword(@NonNull String password) {
        return passwordManager.verifyPassword(password);
    }

    /**
     * Sets a new password and derives passphrase from it.
     * Package-private to restrict direct access.
     *
     * @param password New password
     * @return true if successful
     */
    boolean setPassword(@NonNull String password) {
        return passwordManager.setPasswordInternal(password);
    }

    /**
     * Generates a random password for initial setup.
     *
     * @return Random password
     */
    @NonNull
    public String generateRandomPassword() {
        return passwordManager.generateRandomPassword();
    }

    /**
     * Gets the password manager for advanced operations.
     *
     * @return PasswordDerivationManager instance
     */
    @NonNull
    public PasswordDerivationManager getPasswordManager() {
        return passwordManager;
    }

    @Nullable
    private byte[] loadLegacyPassphrase() {
        String encryptedBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null);
        String ivBase64 = prefs.getString(KEY_PASSPHRASE_IV, null);

        if (encryptedBase64 != null && ivBase64 != null) {
            try {
                byte[] encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP);
                byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);
                byte[] decrypted = decryptWithKeyStore(iv, encrypted);
                if (decrypted != null && decrypted.length == AppConstants.PASSPHRASE_BYTE_LENGTH) {
                    return decrypted;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt legacy passphrase: " + e.getMessage(), e);
            }
        }

        // Fallback for JVM tests
        String fallbackBase64 = prefs.getString(FALLBACK_KEY_PASSPHRASE, null);
        if (fallbackBase64 != null) {
            try {
                byte[] decoded = Base64.decode(fallbackBase64, Base64.NO_WRAP);
                if (decoded != null && decoded.length == AppConstants.PASSPHRASE_BYTE_LENGTH) {
                    return decoded;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode fallback passphrase: " + e.getMessage(), e);
            }
        }

        return null;
    }

    @NonNull
    private byte[] generateNewPassphrase() {
        SecureRandom random = new SecureRandom();
        byte[] newKey = new byte[AppConstants.PASSPHRASE_BYTE_LENGTH];
        random.nextBytes(newKey);

        saveLegacyPassphrase(newKey);
        return newKey;
    }

    private void saveLegacyPassphrase(@NonNull byte[] passphrase) {
        EncryptedData encrypted = encryptWithKeyStore(passphrase);
        if (encrypted != null) {
            prefs.edit()
                    .putString(KEY_PASSPHRASE_IV, Base64.encodeToString(encrypted.iv(), Base64.NO_WRAP))
                    .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encrypted.ciphertext(), Base64.NO_WRAP))
                    .apply();
        } else {
            // JVM test fallback
            prefs.edit()
                    .putString(FALLBACK_KEY_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
                    .apply();
        }
    }

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
                KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                LEGACY_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build();

        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }

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
            Log.w(TAG, "KeyStore encryption not available");
            return null;
        }
    }

    @Nullable
    private byte[] decryptWithKeyStore(@NonNull byte[] iv, @NonNull byte[] ciphertext) {
        try {
            SecretKey secretKey = getOrCreateKeyStoreMasterKey();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(AppConstants.GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            return cipher.doFinal(ciphertext);
        } catch (Throwable e) {
            Log.w(TAG, "KeyStore decryption not available");
            return null;
        }
    }

    /**
     * Converts byte array to hex string.
     *
     * @param bytes Byte array
     * @return Hex string
     */
    @NonNull
    public static String bytesToHex(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Container for encrypted data with IV and ciphertext.
     */
    public record EncryptedData(byte[] iv, byte[] ciphertext) {
    }
}
