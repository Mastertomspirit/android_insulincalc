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
 * The salt is derived from the password to ensure the same password always produces the same salt.
 * Uses SHA-256 hashing with bitwise operations, rotations, and special character insertion.
 */
final class SaltGenerator {

    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final int SALT_LENGTH = 32;
    private static final int HASH_ITERATIONS = 3;

    private SaltGenerator() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a deterministic salt from the given password.
     * Uses multiple iterations of SHA-256 hashing with bitwise operations, rotations,
     * and special character insertion to create a complex, password-derived salt.
     *
     * @param password User password
     * @return 32-byte salt derived from password
     */
    @NonNull
    static byte[] generate(@NonNull String password) {
        try {
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] currentHash = passwordBytes;

            // Multiple hash iterations with increasing complexity
            for (int iteration = 0; iteration < HASH_ITERATIONS; iteration++) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                
                // XOR with iteration number and password length
                byte[] input = new byte[currentHash.length + 4];
                System.arraycopy(currentHash, 0, input, 0, currentHash.length);
                input[currentHash.length] = (byte) iteration;
                input[currentHash.length + 1] = (byte) (password.length() >> 8);
                input[currentHash.length + 2] = (byte) password.length();
                input[currentHash.length + 3] = (byte) (iteration * password.length());
                
                currentHash = digest.digest(input);
            }

            // Apply bitwise transformations
            byte[] transformed = new byte[SALT_LENGTH];
            System.arraycopy(currentHash, 0, transformed, 0, Math.min(currentHash.length, SALT_LENGTH));

            // Bit rotations with varying amounts
            for (int i = 0; i < transformed.length; i++) {
                int rotation = (i * 7 + password.length() * 3) % 8;
                transformed[i] = (byte) Integer.rotateLeft(transformed[i] & 0xFF, rotation);
            }

            // XOR with password bytes (cyclic)
            for (int i = 0; i < transformed.length; i++) {
                transformed[i] = (byte) (transformed[i] ^ passwordBytes[i % passwordBytes.length]);
            }

            // Insert special characters at calculated positions based on password content
            for (int i = 0; i < Math.min(password.length(), 16); i++) {
                char c = password.charAt(i);
                int pos = (c * (i + 1) + password.length() * 13) % SALT_LENGTH;
                if (pos < SALT_LENGTH) {
                    int specialIndex = (c + i * password.length()) % SPECIAL_CHARS.length();
                    transformed[pos] = (byte) SPECIAL_CHARS.charAt(specialIndex);
                }
            }

            // Final mixing pass
            for (int i = 0; i < transformed.length - 1; i++) {
                transformed[i] = (byte) (transformed[i] ^ transformed[i + 1] ^ passwordBytes[i % passwordBytes.length]);
            }

            return transformed;
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
