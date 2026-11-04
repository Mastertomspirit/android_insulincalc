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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import network.spiritscorp.util.AppConstants;

/**
 * Manages password-based derivation of database encryption keys.
 * Uses PBKDF2 with password-derived salt to create deterministic 256-bit keys
 * stored in Android KeyStore for hardware-backed security.
 */
public class PasswordDerivationManager {

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final int PASSPHRASE_BYTE_LENGTH = 32;

    /**
     * Derives a 256-bit database passphrase from user password.
     *
     * @param userPassword User-provided password (must be >= MIN_PASSWORD_LENGTH characters)
     * @return 32-byte database passphrase
     * @throws IllegalArgumentException if password is too short
     */
    @NonNull
    public byte[] derivePassphrase(@NonNull String userPassword) {
        if (userPassword.length() < AppConstants.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password too short. Minimum " + AppConstants.MIN_PASSWORD_LENGTH + " characters required.");
        }

        byte[] salt = SaltGenerator.generate(userPassword);
        return deriveKey(userPassword, salt);
    }

    /**
     * Derives key using PBKDF2 with given salt.
     *
     * @param password User password
     * @param salt Salt bytes
     * @return 32-byte derived key
     */
    @NonNull
    private byte[] deriveKey(@NonNull String password, @NonNull byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    AppConstants.PBKDF2_ITERATIONS,
                    AppConstants.KEY_SIZE_BITS
            );
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();

            if (derivedKey.length != PASSPHRASE_BYTE_LENGTH) {
                throw new IllegalStateException("Derived key has unexpected length: " + derivedKey.length);
            }

            return derivedKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to derive passphrase: " + e.getMessage(), e);
        }
    }

    /**
     * Stores the database passphrase in Android KeyStore.
     *
     * @param dbPassphrase 32-byte database passphrase to store
     * @return true if storage succeeded, false otherwise
     */
    public boolean storeInKeyStore(@NonNull byte[] dbPassphrase) {
        if (dbPassphrase.length != PASSPHRASE_BYTE_LENGTH) {
            return false;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            if (keyStore.containsAlias(AppConstants.DB_PASSPHRASE_KEY_ALIAS)) {
                keyStore.deleteEntry(AppConstants.DB_PASSPHRASE_KEY_ALIAS);
            }

            SecretKey dbPassphraseKey = new SecretKeySpec(dbPassphrase, "AES");
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(dbPassphraseKey);
            keyStore.setEntry(AppConstants.DB_PASSPHRASE_KEY_ALIAS, secretKeyEntry, null);

            return true;
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    /**
     * Loads the database passphrase from Android KeyStore.
     *
     * @return 32-byte database passphrase, or null if not found
     */
    @Nullable
    public byte[] loadFromKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            if (keyStore.containsAlias(AppConstants.DB_PASSPHRASE_KEY_ALIAS)) {
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(
                        AppConstants.DB_PASSPHRASE_KEY_ALIAS, null);
                if (entry != null) {
                    byte[] passphrase = entry.getSecretKey().getEncoded();
                    if (passphrase != null && passphrase.length == PASSPHRASE_BYTE_LENGTH) {
                        return passphrase;
                    }
                }
            }
            return null;
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Verifies if a password matches the stored passphrase.
     *
     * @param userPassword Password to verify
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(@NonNull String userPassword) {
        try {
            byte[] derivedPassphrase = derivePassphrase(userPassword);
            byte[] storedPassphrase = loadFromKeyStore();
            
            if (derivedPassphrase == null || storedPassphrase == null) {
                return false;
            }
            
            return constantTimeCompare(derivedPassphrase, storedPassphrase);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Sets a new password and stores the derived passphrase in KeyStore.
     * Package-private to restrict direct access.
     *
     * @param userPassword Password to set
     * @return true if successful, false otherwise
     */
    boolean setPassword(@NonNull String userPassword) {
        try {
            byte[] dbPassphrase = derivePassphrase(userPassword);
            return storeInKeyStore(dbPassphrase);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Generates a random password for new installations.
     *
     * @return Random password with at least MIN_PASSWORD_LENGTH characters
     */
    @NonNull
    public String generateRandomPassword() {
        return RandomPasswordGenerator.generate();
    }

    /**
     * Clears the stored passphrase from KeyStore.
     * Package-private to restrict direct access.
     *
     * @return true if successful, false otherwise
     */
    boolean clearPassphrase() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            
            if (keyStore.containsAlias(AppConstants.DB_PASSPHRASE_KEY_ALIAS)) {
                keyStore.deleteEntry(AppConstants.DB_PASSPHRASE_KEY_ALIAS);
            }
            return true;
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     *
     * @param a First byte array
     * @param b Second byte array
     * @return true if arrays are equal
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
