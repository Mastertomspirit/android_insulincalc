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

import java.security.SecureRandom;

import network.spiritscorp.util.AppConstants;

/**
 * Generates secure random passwords for database encryption.
 */
final class RandomPasswordGenerator {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALL_CHARS = LOWERCASE + UPPERCASE + DIGITS + SPECIAL_CHARS;

    private RandomPasswordGenerator() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a random password.
     *
     * @return Random password with at least MIN_PASSWORD_LENGTH characters
     */
    @NonNull
    static String generate() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(AppConstants.MIN_PASSWORD_LENGTH + 4);

        // Ensure at least one character from each set
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));

        // Fill the rest with random characters
        for (int i = password.length(); i < AppConstants.MIN_PASSWORD_LENGTH + 4; i++) {
            password.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }

        return shuffleString(password.toString());
    }

    /**
     * Shuffles a string using Fisher-Yates algorithm.
     *
     * @param s String to shuffle
     * @return Shuffled string
     */
    @NonNull
    private static String shuffleString(@NonNull String s) {
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
}
