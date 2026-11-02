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

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Manages password-based derivation of database encryption keys.
 * Uses PBKDF2 with password-derived salt to create deterministic 256-bit keys
 * that are stored in Android KeyStore for hardware-backed security.
 */
public class PasswordDerivationManager {

    private static final String TAG = "PasswordDerivationManager";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "network.spiritscorp.insulincalc.db_passphrase_key";
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int KEY_SIZE_BITS = 256;
    private static final int PASSPHRASE_BYTE_LENGTH = 32;

    // Special characters for salt derivation
    private static final String SALT_SUFFIX = "!@#$%^&*()INSULIN_CALC_2025!";

    /**
     * Derives a 256-bit database passphrase from user password.
     * Uses PBKDF2 with a salt derived from the password itself.
     *
     * @param userPassword User-provided password (must be >= 20 characters).
     * @return 32-byte database passphrase, or null if derivation fails.
     */
    @Nullable
    public byte[] deriveDbPassphrase(@NonNull String userPassword) {
        if (userPassword.length() < 20) {
            Log.w(TAG, "Password too short. Minimum 20 characters required.");
            return null;
        }

        try {
            // Derive salt from password with special characters
            String saltInput = userPassword + SALT_SUFFIX + userPassword.length();
            byte[] salt = saltInput.getBytes(StandardCharsets.UTF_8);

            // Derive key using PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                    userPassword.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    KEY_SIZE_BITS
            );
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();

            // Ensure we have exactly 32 bytes
            if (derivedKey.length != PASSPHRASE_BYTE_LENGTH) {
                Log.e(TAG, "Derived key has unexpected length: " + derivedKey.length);
                return null;
            }

            return derivedKey;
        } catch (Exception e) {
            Log.e(TAG, "Failed to derive passphrase: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Stores the database passphrase in Android KeyStore.
     *
     * @param context Android context.
     * @param dbPassphrase 32-byte database passphrase to store.
     * @return true if storage succeeded, false otherwise.
     */
    public boolean storeInKeyStore(@NonNull android.content.Context context, @NonNull byte[] dbPassphrase) {
        if (dbPassphrase.length != PASSPHRASE_BYTE_LENGTH) {
            Log.e(TAG, "Invalid passphrase length: " + dbPassphrase.length);
            return false;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            // Check if key already exists
            if (keyStore.containsAlias(KEY_ALIAS)) {
                // Key exists, we'll overwrite it
                keyStore.deleteEntry(KEY_ALIAS);
            }

            // Generate a new key in KeyStore
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER
            );

            KeyGenParameterSpec keyGenSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .setRandomizedEncryptionRequired(true)
                    .build();

            keyGenerator.init(keyGenSpec);
            SecretKey secretKey = keyGenerator.generateKey();

            // Store the dbPassphrase as a SecretKey in KeyStore
            // Since KeyStore already generated a key, we need to store our dbPassphrase
            // as a separate entry or use it directly
            // For SQLCipher, we just need the raw bytes, so we'll store them in a
            // custom way. However, KeyStore doesn't support storing arbitrary data.
            // 
            // Solution: We'll store the dbPassphrase as a SecretKey with our alias.
            // But KeyStore requires the key to be generated by it. So we need to
            // import the key.
            
            // Create a SecretKey from our dbPassphrase
            SecretKey dbPassphraseKey = new SecretKeySpec(dbPassphrase, "AES");
            
            // Store it in KeyStore
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(dbPassphraseKey);
            keyStore.setEntry(KEY_ALIAS, secretKeyEntry, null);

            Log.i(TAG, "Successfully stored db passphrase in KeyStore");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to store passphrase in KeyStore: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Loads the database passphrase from Android KeyStore.
     *
     * @return 32-byte database passphrase, or null if not found or loading fails.
     */
    @Nullable
    public byte[] loadFromKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(
                        KEY_ALIAS, null);
                if (entry != null) {
                    byte[] passphrase = entry.getSecretKey().getEncoded();
                    if (passphrase != null && passphrase.length == PASSPHRASE_BYTE_LENGTH) {
                        Log.i(TAG, "Successfully loaded db passphrase from KeyStore");
                        return passphrase;
                    }
                }
            }
            Log.w(TAG, "No db passphrase found in KeyStore");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load passphrase from KeyStore: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Derives and stores the database passphrase in KeyStore.
     *
     * @param context Android context.
     * @param userPassword User-provided password.
     * @return true if derivation and storage succeeded, false otherwise.
     */
    public boolean setPassword(@NonNull android.content.Context context, @NonNull String userPassword) {
        byte[] dbPassphrase = deriveDbPassphrase(userPassword);
        if (dbPassphrase == null) {
            return false;
        }
        return storeInKeyStore(context, dbPassphrase);
    }

    /**
     * Verifies if a password can derive the stored database passphrase.
     *
     * @param userPassword User-provided password to verify.
     * @return true if password is valid, false otherwise.
     */
    public boolean verifyPassword(@NonNull String userPassword) {
        byte[] derivedPassphrase = deriveDbPassphrase(userPassword);
        if (derivedPassphrase == null) {
            return false;
        }

        byte[] storedPassphrase = loadFromKeyStore();
        if (storedPassphrase == null) {
            return false;
        }

        // Compare the derived passphrase with the stored one
        // Use constant-time comparison to prevent timing attacks
        return constantTimeCompare(derivedPassphrase, storedPassphrase);
    }

    /**
     * Clears the password-based key from KeyStore.
     *
     * @return true if key was cleared, false otherwise.
     */
    public boolean clearPassword() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
                Log.i(TAG, "Successfully cleared password from KeyStore");
                return true;
            }
            return true; // Already clear
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear password from KeyStore: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if a password-based key exists in KeyStore.
     *
     * @return true if password is set, false otherwise.
     */
    public boolean isPasswordSet() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            return keyStore.containsAlias(KEY_ALIAS);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check password: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private boolean constantTimeCompare(@NonNull byte[] a, @NonNull byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
