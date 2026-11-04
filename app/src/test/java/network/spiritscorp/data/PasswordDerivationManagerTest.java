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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for PasswordDerivationManager.
 * Tests password derivation, verification, storage, and random password generation.
 */
class PasswordDerivationManagerTest {

    private static final int PASSPHRASE_LENGTH = 32;
    private PasswordDerivationManager manager;

    @BeforeEach
    void setUp() {
        manager = new PasswordDerivationManager();
    }

    // ==================== Derivation Tests ====================

    @Test
    void derivePassphraseReturns32ByteArray() {
        String password = "ThisIsATestPassword1234!";
        byte[] passphrase = manager.derivePassphrase(password);
        
        assertNotNull(passphrase, "Derived passphrase should not be null");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Passphrase should be 32 bytes");
    }

    @Test
    void derivePassphraseIsDeterministic() {
        String password = "ThisIsATestPassword1234!";
        byte[] passphrase1 = manager.derivePassphrase(password);
        byte[] passphrase2 = manager.derivePassphrase(password);
        
        assertNotNull(passphrase1, "First derivation should not be null");
        assertNotNull(passphrase2, "Second derivation should not be null");
        assertArrayEquals(passphrase1, passphrase2, "Same password should produce same passphrase");
    }

    @Test
    void derivePassphraseDifferentPasswordsDifferentOutputs() {
        byte[] passphrase1 = manager.derivePassphrase("PasswordOne12345678!");
        byte[] passphrase2 = manager.derivePassphrase("PasswordTwo12345678!");
        
        assertNotNull(passphrase1, "First passphrase should not be null");
        assertNotNull(passphrase2, "Second passphrase should not be null");
        assertFalse(Arrays.equals(passphrase1, passphrase2), 
                "Different passwords should produce different passphrases");
    }

    @Test
    void derivePassphraseTooShortThrowsException() {
        String shortPassword = "short";
        assertThrows(IllegalArgumentException.class, () -> {
            manager.derivePassphrase(shortPassword);
        }, "Short password should throw IllegalArgumentException");
    }

    @Test
    void derivePassphraseMinimumLengthAccepted() {
        String password = "12345678901234567890"; // Exactly 20 chars
        byte[] passphrase = manager.derivePassphrase(password);
        
        assertNotNull(passphrase, "Password with exactly 20 chars should succeed");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Passphrase should be 32 bytes");
    }

    @Test
    void derivePassphraseWith19CharsFails() {
        String password = "1234567890123456789"; // 19 chars
        assertThrows(IllegalArgumentException.class, () -> {
            manager.derivePassphrase(password);
        }, "Password with 19 chars should fail");
    }

    @Test
    void derivePassphraseWithSpecialCharacters() {
        String password = "P@ssw0rd!With$p3c1@l#Ch@r$";
        byte[] passphrase = manager.derivePassphrase(password);
        
        assertNotNull(passphrase, "Password with special chars should succeed");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Passphrase should be 32 bytes");
    }

    @Test
    void derivePassphraseWithUnicodeChars() {
        String password = "Passw\u00f6rd1234567890!@"; // with umlaut
        byte[] passphrase = manager.derivePassphrase(password);
        
        assertNotNull(passphrase, "Password with unicode chars should succeed");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Passphrase should be 32 bytes");
    }

    @Test
    void derivePassphraseWithFrenchChars() {
        String password = "Passw\u00f4rd1234567890!@"; // with circumflex
        byte[] passphrase = manager.derivePassphrase(password);
        
        assertNotNull(passphrase, "Password with french chars should succeed");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Passphrase should be 32 bytes");
    }

    // ==================== Verification Tests ====================

    @Test
    void verifyPasswordWithCorrectPasswordReturnsTrue() {
        String password = "ThisIsATestPassword1234!";
        manager.setPassword(password);
        
        boolean result = manager.verifyPassword(password);
        assertTrue(result, "Correct password should verify successfully");
    }

    @Test
    void verifyPasswordWithWrongPasswordReturnsFalse() {
        String correctPassword = "ThisIsATestPassword1234!";
        String wrongPassword = "WrongPassword12345678!";
        manager.setPassword(correctPassword);
        
        boolean result = manager.verifyPassword(wrongPassword);
        assertFalse(result, "Wrong password should fail verification");
    }

    @Test
    void verifyPasswordWithTooShortPasswordReturnsFalse() {
        String longPassword = "ThisIsATestPassword1234!";
        String shortPassword = "short";
        manager.setPassword(longPassword);
        
        boolean result = manager.verifyPassword(shortPassword);
        assertFalse(result, "Too short password should fail verification");
    }

    @Test
    void verifyPasswordWithEmptyPasswordReturnsFalse() {
        String password = "ThisIsATestPassword1234!";
        manager.setPassword(password);
        
        boolean result = manager.verifyPassword("");
        assertFalse(result, "Empty password should fail verification");
    }

    // ==================== Password Setting Tests ====================

    @Test
    void setPasswordWithValidPasswordSucceeds() {
        String password = "ThisIsATestPassword1234!";
        boolean result = manager.setPassword(password);
        assertTrue(result, "Valid password should be set successfully");
    }

    @Test
    void setPasswordWithTooShortPasswordFails() {
        String shortPassword = "short";
        boolean result = manager.setPassword(shortPassword);
        assertFalse(result, "Short password should fail to set");
    }

    @Test
    void setPasswordWithEmptyPasswordFails() {
        boolean result = manager.setPassword("");
        assertFalse(result, "Empty password should fail to set");
    }

    // ==================== Storage Tests ====================

    @Test
    void storeAndLoadFromKeyStore() {
        String password = "ThisIsATestPassword1234!";
        byte[] passphrase = manager.derivePassphrase(password);
        
        boolean stored = manager.storeInKeyStore(passphrase);
        assertTrue(stored, "Passphrase should be stored successfully");
        
        byte[] loaded = manager.loadFromKeyStore();
        assertNotNull(loaded, "Passphrase should be loaded successfully");
        assertArrayEquals(passphrase, loaded, "Loaded passphrase should match stored");
    }

    @Test
    void storeInKeyStoreWithInvalidLengthFails() {
        byte[] invalidPassphrase = new byte[16]; // Too short
        boolean result = manager.storeInKeyStore(invalidPassphrase);
        assertFalse(result, "Invalid length passphrase should fail to store");
    }

    @Test
    void storeInKeyStoreWithNullFails() {
        boolean result = manager.storeInKeyStore(null);
        assertFalse(result, "Null passphrase should fail to store");
    }

    @Test
    void loadFromKeyStoreWhenEmptyReturnsNull() {
        byte[] loaded = manager.loadFromKeyStore();
        assertTrue(loaded == null, "Loading from empty KeyStore should return null");
    }

    // ==================== Random Password Tests ====================

    @Test
    void generateRandomPasswordMeetsMinimumLength() {
        String randomPassword = manager.generateRandomPassword();
        
        assertNotNull(randomPassword, "Random password should not be null");
        assertTrue(randomPassword.length() >= 20, 
                "Random password should be at least 20 chars, got " + randomPassword.length());
    }

    @Test
    void generateRandomPasswordContainsLowercase() {
        String randomPassword = manager.generateRandomPassword();
        
        assertNotNull(randomPassword, "Random password should not be null");
        assertTrue(randomPassword.matches(".*[a-z].*"), "Should contain lowercase letters");
    }

    @Test
    void generateRandomPasswordContainsUppercase() {
        String randomPassword = manager.generateRandomPassword();
        
        assertNotNull(randomPassword, "Random password should not be null");
        assertTrue(randomPassword.matches(".*[A-Z].*"), "Should contain uppercase letters");
    }

    @Test
    void generateRandomPasswordContainsDigits() {
        String randomPassword = manager.generateRandomPassword();
        
        assertNotNull(randomPassword, "Random password should not be null");
        assertTrue(randomPassword.matches(".*[0-9].*"), "Should contain digits");
    }

    @Test
    void generateRandomPasswordContainsSpecialChars() {
        String randomPassword = manager.generateRandomPassword();
        
        assertNotNull(randomPassword, "Random password should not be null");
        assertTrue(randomPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*"), 
                "Should contain special characters");
    }

    @Test
    void generateRandomPasswordIsUnique() {
        String password1 = manager.generateRandomPassword();
        String password2 = manager.generateRandomPassword();
        
        assertNotNull(password1, "First password should not be null");
        assertNotNull(password2, "Second password should not be null");
        assertNotEquals(password1, password2, "Random passwords should be unique");
    }

    @Test
    void generateRandomPasswordLengthIsConsistent() {
        // Generate multiple passwords and check they all meet minimum length
        for (int i = 0; i < 10; i++) {
            String password = manager.generateRandomPassword();
            assertNotNull(password, "Password " + i + " should not be null");
            assertTrue(password.length() >= 20, 
                    "Password " + i + " should be at least 20 chars");
        }
    }
}
