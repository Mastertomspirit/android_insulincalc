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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for PasswordDerivationManager.
 */
class PasswordDerivationManagerTest {

    private static final int PASSPHRASE_LENGTH = 32;
    private PasswordDerivationManager manager;

    @BeforeEach
    void setUp() {
        manager = new PasswordDerivationManager();
    }

    @Test
    void deriveDbPassphraseReturns32ByteArray() {
        String password = "ThisIsATestPassword1234!";
        byte[] passphrase = manager.deriveDbPassphrase(password);
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_LENGTH, passphrase.length);
    }

    @Test
    void deriveDbPassphraseIsDeterministic() {
        String password = "ThisIsATestPassword1234!";
        byte[] passphrase1 = manager.deriveDbPassphrase(password);
        byte[] passphrase2 = manager.deriveDbPassphrase(password);
        
        assertNotNull(passphrase1);
        assertNotNull(passphrase2);
        assertArrayEquals(passphrase1, passphrase2);
    }

    @Test
    void deriveDbPassphraseDifferentPasswordsDifferentOutputs() {
        byte[] passphrase1 = manager.deriveDbPassphrase("PasswordOne12345678!");
        byte[] passphrase2 = manager.deriveDbPassphrase("PasswordTwo12345678!");
        
        assertNotNull(passphrase1);
        assertNotNull(passphrase2);
        assertFalse(java.util.Arrays.equals(passphrase1, passphrase2));
    }

    @Test
    void deriveDbPassphraseTooShortThrowsException() {
        String shortPassword = "short";
        assertThrows(IllegalArgumentException.class, () -> {
            manager.deriveDbPassphrase(shortPassword);
        });
    }

    @Test
    void deriveDbPassphraseMinimumLengthAccepted() {
        String password = "12345678901234567890"; // Exactly 20 chars
        byte[] passphrase = manager.deriveDbPassphrase(password);
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_LENGTH, passphrase.length);
    }

    @Test
    void deriveDbPassphraseWithSpecialCharacters() {
        String password = "P@ssw0rd!With$p3c1@l#Ch@r$";
        byte[] passphrase = manager.deriveDbPassphrase(password);
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_LENGTH, passphrase.length);
    }

    @Test
    void generateRandomPasswordMeetsMinimumLength() {
        String randomPassword = manager.generateRandomPassword();
        
        assertNotNull(randomPassword);
        assertTrue(randomPassword.length() >= 20);
    }

    @Test
    void generateRandomPasswordContainsVariousCharacterTypes() {
        String randomPassword = manager.generateRandomPassword();
        
        assertNotNull(randomPassword);
        assertTrue(randomPassword.matches(".*[a-z].*"));
        assertTrue(randomPassword.matches(".*[A-Z].*"));
        assertTrue(randomPassword.matches(".*[0-9].*"));
        assertTrue(randomPassword.matches(".*[!@#$%^&*()_+\-=\[\]{}|;:,.<>?].*"));
    }

    @Test
    void generateRandomPasswordIsUnique() {
        String password1 = manager.generateRandomPassword();
        String password2 = manager.generateRandomPassword();
        
        assertNotNull(password1);
        assertNotNull(password2);
        assertNotEquals(password1, password2);
    }

    @Test
    void verifyPasswordWithCorrectPasswordReturnsTrue() {
        String password = "ThisIsATestPassword1234!";
        manager.setPasswordInternal(password);
        
        boolean result = manager.verifyPassword(password);
        assertTrue(result);
    }

    @Test
    void verifyPasswordWithWrongPasswordReturnsFalse() {
        String correctPassword = "ThisIsATestPassword1234!";
        String wrongPassword = "WrongPassword12345678!";
        manager.setPasswordInternal(correctPassword);
        
        boolean result = manager.verifyPassword(wrongPassword);
        assertFalse(result);
    }

    @Test
    void verifyPasswordWithTooShortPasswordReturnsFalse() {
        String longPassword = "ThisIsATestPassword1234!";
        String shortPassword = "short";
        manager.setPasswordInternal(longPassword);
        
        boolean result = manager.verifyPassword(shortPassword);
        assertFalse(result);
    }

    @Test
    void setPasswordInternalWithValidPasswordSucceeds() {
        String password = "ThisIsATestPassword1234!";
        boolean result = manager.setPasswordInternal(password);
        assertTrue(result);
    }

    @Test
    void setPasswordInternalWithTooShortPasswordFails() {
        String shortPassword = "short";
        boolean result = manager.setPasswordInternal(shortPassword);
        assertFalse(result);
    }

    @Test
    void storeAndLoadFromKeyStore() {
        String password = "ThisIsATestPassword1234!";
        byte[] passphrase = manager.deriveDbPassphrase(password);
        
        boolean stored = manager.storeInKeyStore(passphrase);
        assertTrue(stored);
        
        byte[] loaded = manager.loadFromKeyStore();
        assertNotNull(loaded);
        assertArrayEquals(passphrase, loaded);
    }
}
