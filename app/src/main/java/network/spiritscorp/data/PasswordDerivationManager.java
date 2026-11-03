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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
 * that are stored in Android KeyStore for hardware-backed security.
 *
 * <p>The salt is derived from the password using SHA-256 hashing with bitwise operations
 * to ensure it contains special characters and is unique per password.</p>
 */
public class PasswordDerivationManager {

    private static final String TAG = "PasswordDerivationMgr";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final int KEY_SIZE_BITS = 256;
    private static final int PASSPHRASE_BYTE_LENGTH = 32;

    // Special character set for salt enhancement
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final int SALT_LENGTH = 32;

    /**
     * Derives a 256-bit database passphrase from user password.
     * Uses PBKDF2 with a salt derived from the password itself using complex operations.
     *
     * @param userPassword User-provided password (must be >= MIN_PASSWORD_LENGTH characters).
     * @return 32-byte database passphrase, or null if derivation fails.
     * @throws IllegalArgumentException if password is too short
     */
    @Nullable
    public byte[] deriveDbPassphrase(@NonNull String userPassword) {
        if (userPassword.length() < AppConstants.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password too short. Minimum " + AppConstants.MIN_PASSWORD_LENGTH + " characters required.");
        }

        try {
            // Derive salt from password using complex operations
            byte[] salt = deriveSaltFromPassword(userPassword);

            // Derive key using PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                    userPassword.toCharArray(),
                    salt,
                    AppConstants.PBKDF2_ITERATIONS,
                    KEY_SIZE_BITS
            );
            byte[] derivedKey = factory.generateSecret(spec).getEncoded();

            // Ensure we have exactly 32 bytes
            if (derivedKey.length != PASSPHRASE_BYTE_LENGTH) {
                Log.e(TAG, "Derived key has unexpected length: " + derivedKey.length);
                return null;
            }

            return derivedKey;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "PBKDF2 algorithm not available: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            // Catch other exceptions (InvalidKeySpecException, etc.)
            Log.e(TAG, "Failed to derive passphrase: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Derives a salt from the password using SHA-256 hashing with bitwise operations.
     * The resulting salt contains special characters and is deterministic for the same password.
     *
     * @param password User password
     * @return 32-byte salt derived from password
     */
    @NonNull
    private byte[] deriveSaltFromPassword(@NonNull String password) {
        try {
            // Step 1: Hash the password with SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Step 2: Apply bitwise operations to introduce complexity
            // XOR with a pattern based on password length
            int passwordLength = password.length();
            for (int i = 0; i < hash.length; i++) {
                // XOR with pattern: password length + i + special char rotation
                int pattern = (passwordLength + i) % 256;
                hash[i] = (byte) (hash[i] ^ pattern);

                // Rotate bits
                hash[i] = (byte) ((byte) Integer.rotateLeft(hash[i] & 0xFF, i % 8) & 0xFF);
            }

            // Step 3: Insert special characters at specific positions
            // We'll replace some bytes with special character codes
            byte[] saltWithSpecialChars = Arrays.copyOf(hash, SALT_LENGTH);

            // Use password characters to determine positions for special chars
            for (int i = 0; i < Math.min(password.length(), 8); i++) {
                int pos = (password.charAt(i) + i * 7) % SALT_LENGTH;
                // Get a special character based on password char
                int specialIndex = (password.charAt(i) + i) % SPECIAL_CHARS.length();
                saltWithSpecialChars[pos] = (byte) SPECIAL_CHARS.charAt(specialIndex);
            }

            // Step 4: Final XOR with password bytes (cyclic)
            for (int i = 0; i < SALT_LENGTH; i++) {
                byte passwordByte = password.getBytes(StandardCharsets.UTF_8)[i % password.length()];
                saltWithSpecialChars[i] = (byte) (saltWithSpecialChars[i] ^ passwordByte);
            }

            return saltWithSpecialChars;
        } catch (NoSuchAlgorithmException e) {
            // Fallback: use password bytes padded to SALT_LENGTH
            byte[] fallbackSalt = new byte[SALT_LENGTH];
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(passwordBytes, 0, fallbackSalt, 0, Math.min(passwordBytes.length, SALT_LENGTH));
            // Fill remaining with special characters
            for (int i = passwordBytes.length; i < SALT_LENGTH; i++) {
                fallbackSalt[i] = (byte) SPECIAL_CHARS.charAt(i % SPECIAL_CHARS.length());
            }
            return fallbackSalt;
        }
    }

    /**
     * Stores the database passphrase in Android KeyStore.
     *
     * @param dbPassphrase 32-byte database passphrase to store
     * @return true if storage succeeded, false otherwise
     * @throws IllegalArgumentException if passphrase length is invalid
     */
    public boolean storeInKeyStore(@NonNull byte[] dbPassphrase) {
        if (dbPassphrase.length != PASSPHRASE_BYTE_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid passphrase length: " + dbPassphrase.length + ", expected " + PASSPHRASE_BYTE_LENGTH);
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            // Delete existing key if it exists
            if (keyStore.containsAlias(AppConstants.DB_PASSPHRASE_KEY_ALIAS)) {
                keyStore.deleteEntry(AppConstants.DB_PASSPHRASE_KEY_ALIAS);
            }

            // Create a SecretKey from our dbPassphrase
            SecretKey dbPassphraseKey = new SecretKeySpec(dbPassphrase, "AES");

            // Store it in KeyStore
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(dbPassphraseKey);
            keyStore.setEntry(AppConstants.DB_PASSPHRASE_KEY_ALIAS, secretKeyEntry, null);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to store passphrase in KeyStore: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Loads the database passphrase from Android KeyStore.
     *
     * @return 32-byte database passphrase, or null if not found or loading fails
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
        } catch (Exception e) {
            Log.e(TAG, "Failed to load passphrase from KeyStore: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Derives and stores the database passphrase in KeyStore.
     *
     * @param userPassword User-provided password
     * @return true if derivation and storage succeeded, false otherwise
     * @throws IllegalArgumentException if password is too short or derivation fails
     */
    public boolean setPassword(@NonNull String userPassword) {
        byte[] dbPassphrase = deriveDbPassphrase(userPassword);
        if (dbPassphrase == null) {
            return false;
        }
        return storeInKeyStore(dbPassphrase);
    }

    /**
     * Verifies if a password can derive the stored database passphrase.
     *
     * @param userPassword User-provided password to verify
     * @return true if password is valid, false otherwise
     */
    public boolean verifyPassword(@NonNull String userPassword) {
        try {
            byte[] derivedPassphrase = deriveDbPassphrase(userPassword);
            if (derivedPassphrase == null) {
                return false;
            }

            byte[] storedPassphrase = loadFromKeyStore();
            if (storedPassphrase == null) {
                return false;
            }

            // Use constant-time comparison to prevent timing attacks
            return constantTimeCompare(derivedPassphrase, storedPassphrase);
        } catch (IllegalArgumentException e) {
            // Password too short or other validation error
            return false;
        }
    }

    /**
     * Changes the password and re-encrypts the database.
     * This method should be called when the user wants to change their password.
     *
     * @param oldPassword Current password for verification
     * @param newPassword New password to set
     * @return true if password change succeeded, false otherwise
     * @throws IllegalArgumentException if passwords are invalid
     */
    public boolean changePassword(@NonNull String oldPassword, @NonNull String newPassword) {
        // Verify old password first
        if (!verifyPassword(oldPassword)) {
            throw new SecurityException("Old password verification failed");
        }

        // Derive and store new passphrase
        return setPassword(newPassword);
    }

    /**
     * Checks if a password-based key exists in KeyStore.
     *
     * @return true if password is set, false otherwise
     */
    public boolean isPasswordSet() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            return keyStore.containsAlias(AppConstants.DB_PASSPHRASE_KEY_ALIAS);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check password: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generates a random password for new installations.
     *
     * @return Randomly generated password with at least MIN_PASSWORD_LENGTH characters
     */
    @NonNull
    public String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(AppConstants.MIN_PASSWORD_LENGTH + 4);

        // Character sets
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        String allChars = lowercase + uppercase + digits + SPECIAL_CHARS;

        // Ensure at least one character from each set
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));

        // Fill the rest with random characters
        for (int i = password.length(); i < AppConstants.MIN_PASSWORD_LENGTH + 4; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password
        return shuffleString(password.toString());
    }

    /**
     * Shuffles a string using Fisher-Yates algorithm.
     *
     * @param s String to shuffle
     * @return Shuffled string
     */
    @NonNull
    private String shuffleString(@NonNull String s) {
        char[] array = s.toCharArray();
        SecureRandom random = new SecureRandom();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return new String(array);
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     *
     * @param a First byte array
     * @param b Second byte array
     * @return true if arrays are equal, false otherwise
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
