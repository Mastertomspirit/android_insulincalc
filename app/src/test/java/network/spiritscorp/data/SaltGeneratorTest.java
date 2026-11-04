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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for SaltGenerator.
 */
class SaltGeneratorTest {

    private static final int SALT_LENGTH = 32;

    @Test
    void generateReturns32ByteSalt() {
        byte[] salt = SaltGenerator.generate("TestPassword12345678!");
        assertNotNull(salt);
        assertEquals(SALT_LENGTH, salt.length);
    }

    @Test
    void generateIsDeterministic() {
        String password = "TestPassword12345678!";
        byte[] salt1 = SaltGenerator.generate(password);
        byte[] salt2 = SaltGenerator.generate(password);
        
        assertNotNull(salt1);
        assertNotNull(salt2);
        assertTrue(java.util.Arrays.equals(salt1, salt2));
    }

    @Test
    void generateDifferentPasswordsDifferentSalts() {
        byte[] salt1 = SaltGenerator.generate("PasswordOne12345678!");
        byte[] salt2 = SaltGenerator.generate("PasswordTwo12345678!");
        
        assertNotNull(salt1);
        assertNotNull(salt2);
        assertTrue(!java.util.Arrays.equals(salt1, salt2));
    }

    @Test
    void generateWithSpecialCharacters() {
        byte[] salt = SaltGenerator.generate("P@ssw0rd!With$p3c1@l#");
        assertNotNull(salt);
        assertEquals(SALT_LENGTH, salt.length);
    }

    @Test
    void generateWithMinimumLengthPassword() {
        String password = "12345678901234567890"; // Exactly 20 chars
        byte[] salt = SaltGenerator.generate(password);
        assertNotNull(salt);
        assertEquals(SALT_LENGTH, salt.length);
    }
}
