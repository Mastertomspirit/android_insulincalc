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

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Generates deterministic salts from passwords using complex cryptographic operations.
 * Salt is derived from password to ensure same password always produces same salt.
 */
final class SaltGenerator {

    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final int SALT_LENGTH = 32;

    private SaltGenerator() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a deterministic salt from the given password.
     * Uses SHA-256 hashing with bitwise operations and special character insertion.
     *
     * @param password User password
     * @return 32-byte salt derived from password
     */
    @NonNull
    static byte[] generate(@NonNull String password) {
        try {
            // Step 1: Hash password with SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Step 2: Apply bitwise XOR with password length pattern
            int passwordLength = password.length();
            for (int i = 0; i < hash.length; i++) {
                int pattern = (passwordLength + i) % 256;
                hash[i] = (byte) (hash[i] ^ pattern);
            }

            // Step 3: Apply bit rotation
            for (int i = 0; i < hash.length; i++) {
                hash[i] = (byte) Integer.rotateLeft(hash[i] & 0xFF, i % 8);
            }

            // Step 4: Ensure exactly SALT_LENGTH bytes
            byte[] salt = Arrays.copyOf(hash, SALT_LENGTH);

            // Step 5: Insert special characters at calculated positions
            for (int i = 0; i < Math.min(password.length(), 8); i++) {
                int pos = (password.charAt(i) + i * 7) % SALT_LENGTH;
                if (pos < SALT_LENGTH) {
                    int specialIndex = (password.charAt(i) + i) % SPECIAL_CHARS.length();
                    salt[pos] = (byte) SPECIAL_CHARS.charAt(specialIndex);
                }
            }

            // Step 6: Final XOR with password bytes (cyclic)
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < SALT_LENGTH; i++) {
                salt[i] = (byte) (salt[i] ^ passwordBytes[i % passwordBytes.length]);
            }

            return salt;
        } catch (NoSuchAlgorithmException e) {
            // Fallback: use password bytes padded with special characters
            return createFallbackSalt(password);
        }
    }

    @NonNull
    private static byte[] createFallbackSalt(@NonNull String password) {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        
        System.arraycopy(passwordBytes, 0, salt, 0, 
                Math.min(passwordBytes.length, SALT_LENGTH));
        
        for (int i = passwordBytes.length; i < SALT_LENGTH; i++) {
            salt[i] = (byte) SPECIAL_CHARS.charAt(i % SPECIAL_CHARS.length());
        }
        
        return salt;
    }
}
