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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for RandomPasswordGenerator.
 */
class RandomPasswordGeneratorTest {

    private static final int MIN_LENGTH = 20;

    @Test
    void generateReturnsNonNullPassword() {
        String password = RandomPasswordGenerator.generate();
        assertNotNull(password);
    }

    @Test
    void generateMeetsMinimumLength() {
        String password = RandomPasswordGenerator.generate();
        assertNotNull(password);
        assertTrue(password.length() >= MIN_LENGTH);
    }

    @Test
    void generateContainsLowercase() {
        String password = RandomPasswordGenerator.generate();
        assertNotNull(password);
        assertTrue(password.matches(".*[a-z].*"));
    }

    @Test
    void generateContainsUppercase() {
        String password = RandomPasswordGenerator.generate();
        assertNotNull(password);
        assertTrue(password.matches(".*[A-Z].*"));
    }

    @Test
    void generateContainsDigits() {
        String password = RandomPasswordGenerator.generate();
        assertNotNull(password);
        assertTrue(password.matches(".*[0-9].*"));
    }

    @Test
    void generateContainsSpecialChars() {
        String password = RandomPasswordGenerator.generate();
        assertNotNull(password);
        assertTrue(password.matches(".*[!@#$%^&*()_+\-=\[\]{}|;:,.<>?].*"));
    }

    @Test
    void generateIsUnique() {
        String password1 = RandomPasswordGenerator.generate();
        String password2 = RandomPasswordGenerator.generate();
        
        assertNotNull(password1);
        assertNotNull(password2);
        assertNotEquals(password1, password2);
    }
}
