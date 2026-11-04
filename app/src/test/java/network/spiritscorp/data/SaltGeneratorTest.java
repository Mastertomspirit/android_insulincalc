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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for SaltGenerator.
 * Tests deterministic salt generation from passwords.
 */
class SaltGeneratorTest {

    private static final int SALT_LENGTH = 32;

    @Test
    void generateReturnsNonNullSalt() {
        String password = "TestPassword1234!";
        byte[] salt = SaltGenerator.generate(password);
        assertNotNull(salt, "Salt should not be null");
    }

    @Test
    void generateReturnsCorrectLength() {
        String password = "TestPassword1234!";
        byte[] salt = SaltGenerator.generate(password);
        assertEquals(SALT_LENGTH, salt.length, "Salt should be 32 bytes");
    }

    @Test
    void generateIsDeterministic() {
        String password = "TestPassword1234!";
        byte[] salt1 = SaltGenerator.generate(password);
        byte[] salt2 = SaltGenerator.generate(password);
        
        assertNotNull(salt1, "First salt should not be null");
        assertNotNull(salt2, "Second salt should not be null");
        
        // Compare byte arrays
        boolean equal = true;
        if (salt1.length != salt2.length) {
            equal = false;
        } else {
            for (int i = 0; i < salt1.length; i++) {
                if (salt1[i] != salt2[i]) {
                    equal = false;
                    break;
                }
            }
        }
        assertTrue(equal, "Same password should produce same salt");
    }

    @Test
    void generateDifferentPasswordsDifferentSalts() {
        byte[] salt1 = SaltGenerator.generate("PasswordOne1234!");
        byte[] salt2 = SaltGenerator.generate("PasswordTwo1234!");
        
        assertNotNull(salt1, "First salt should not be null");
        assertNotNull(salt2, "Second salt should not be null");
        
        // Check that salts are different
        boolean different = false;
        if (salt1.length != salt2.length) {
            different = true;
        } else {
            for (int i = 0; i < salt1.length; i++) {
                if (salt1[i] != salt2[i]) {
                    different = true;
                    break;
                }
            }
        }
        assertTrue(different, "Different passwords should produce different salts");
    }

    @Test
    void generateWithSpecialCharacters() {
        String password = "P@ssw0rd!With$pecial#Chars";
        byte[] salt = SaltGenerator.generate(password);
        
        assertNotNull(salt, "Salt with special chars should not be null");
        assertEquals(SALT_LENGTH, salt.length, "Salt should be 32 bytes");
    }

    @Test
    void generateWithUnicodeCharacters() {
        String password = "Passw\u00f6rd1234567890!@"; // with umlaut
        byte[] salt = SaltGenerator.generate(password);
        
        assertNotNull(salt, "Salt with unicode should not be null");
        assertEquals(SALT_LENGTH, salt.length, "Salt should be 32 bytes");
    }

    @Test
    void generateWithEmptyPassword() {
        String password = "";
        byte[] salt = SaltGenerator.generate(password);
        
        assertNotNull(salt, "Salt with empty password should not be null");
        assertEquals(SALT_LENGTH, salt.length, "Salt should be 32 bytes");
    }

    @Test
    void generateWithShortPassword() {
        String password = "short";
        byte[] salt = SaltGenerator.generate(password);
        
        assertNotNull(salt, "Salt with short password should not be null");
        assertEquals(SALT_LENGTH, salt.length, "Salt should be 32 bytes");
    }
}
