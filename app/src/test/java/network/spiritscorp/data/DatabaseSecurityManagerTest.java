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

import android.content.SharedPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for DatabaseSecurityManager.
 * Tests focus on passphrase generation, derivation, and utility methods.
 */
class DatabaseSecurityManagerTest {

    private static final int PASSPHRASE_LENGTH = 32;
    private SharedPreferences mockPrefs;
    private DatabaseSecurityManager securityManager;

    @BeforeEach
    void setUp() {
        mockPrefs = Mockito.mock(SharedPreferences.class);
        Mockito.when(mockPrefs.getString(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
        Mockito.when(mockPrefs.edit()).thenReturn(Mockito.mock(SharedPreferences.Editor.class));
        
        securityManager = new DatabaseSecurityManager(mockPrefs);
    }

    @Test
    void getDatabasePassphraseReturns32ByteArray() {
        byte[] passphrase = securityManager.getDatabasePassphrase();
        
        assertNotNull(passphrase, "Passphrase should not be null");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Passphrase should be 32 bytes");
    }

    @Test
    void getDatabasePassphraseIsConsistent() {
        byte[] passphrase1 = securityManager.getDatabasePassphrase();
        byte[] passphrase2 = securityManager.getDatabasePassphrase();
        
        assertNotNull(passphrase1, "First passphrase should not be null");
        assertNotNull(passphrase2, "Second passphrase should not be null");
        assertArrayEquals(passphrase1, passphrase2, "Passphrases should be consistent");
    }

    @Test
    void derivePassphraseReturns32ByteArray() {
        String password = "ThisIsATestPassword1234!";
        byte[] passphrase = securityManager.derivePassphrase(password);
        
        assertNotNull(passphrase, "Derived passphrase should not be null");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Derived passphrase should be 32 bytes");
    }

    @Test
    void derivePassphraseWithTooShortPasswordReturnsNull() {
        String shortPassword = "short";
        byte[] passphrase = securityManager.derivePassphrase(shortPassword);
        
        assertTrue(passphrase == null, "Short password should return null");
    }

    @Test
    void derivePassphraseWithExactly20CharsSucceeds() {
        String password = "12345678901234567890"; // exactly 20 chars
        byte[] passphrase = securityManager.derivePassphrase(password);
        
        assertNotNull(passphrase, "Password with exactly 20 chars should succeed");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Passphrase should be 32 bytes");
    }

    @Test
    void derivePassphraseWith19CharsFails() {
        String password = "1234567890123456789"; // 19 chars
        byte[] passphrase = securityManager.derivePassphrase(password);
        
        assertTrue(passphrase == null, "Password with 19 chars should fail");
    }

    @Test
    void derivePassphraseWithSpecialChars() {
        String password = "Pässwörd!1234567890@#"; // with umlauts and special chars
        byte[] passphrase = securityManager.derivePassphrase(password);
        
        assertNotNull(passphrase, "Password with special chars should succeed");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Passphrase should be 32 bytes");
    }

    @Test
    void derivePassphraseWithUnicodeChars() {
        String password = "Passwörd1234567890!@"; // with umlaut
        byte[] passphrase = securityManager.derivePassphrase(password);
        
        assertNotNull(passphrase, "Password with unicode chars should succeed");
        assertEquals(PASSPHRASE_LENGTH, passphrase.length, "Passphrase should be 32 bytes");
    }

    @Test
    void generateRandomPasswordMeetsMinimumLength() {
        String randomPassword = securityManager.generateRandomPassword();
        
        assertNotNull(randomPassword, "Random password should not be null");
        assertTrue(randomPassword.length() >= 20, "Random password should be at least 20 chars");
    }

    @Test
    void generateRandomPasswordContainsVariousCharacterTypes() {
        String randomPassword = securityManager.generateRandomPassword();
        
        assertNotNull(randomPassword, "Random password should not be null");
        assertTrue(randomPassword.matches(".*[a-z].*"), "Should contain lowercase");
        assertTrue(randomPassword.matches(".*[A-Z].*"), "Should contain uppercase");
        assertTrue(randomPassword.matches(".*[0-9].*"), "Should contain digits");
        assertTrue(randomPassword.matches(".*[!@#$%^&*()_+-=\[\]{}|;:,.<>?].*"), "Should contain special chars");
    }

    @Test
    void getPasswordManagerReturnsNonNull() {
        PasswordDerivationManager manager = securityManager.getPasswordManager();
        assertNotNull(manager, "Password manager should not be null");
    }

    @Test
    void bytesToHexWithEmptyArrayReturnsEmptyString() {
        String hex = DatabaseSecurityManager.bytesToHex(new byte[0]);
        assertEquals("", hex, "Empty array should return empty string");
    }

    @Test
    void bytesToHexWithNullReturnsEmptyString() {
        String hex = DatabaseSecurityManager.bytesToHex(null);
        assertEquals("", hex, "Null should return empty string");
    }

    @Test
    void bytesToHexWithSampleData() {
        byte[] bytes = new byte[]{(byte) 0x00, (byte) 0x0F, (byte) 0x1A, (byte) 0xFF};
        String hex = DatabaseSecurityManager.bytesToHex(bytes);
        assertEquals("000f1aff", hex, "Hex conversion should be correct");
    }

    @Test
    void bytesToHexWithAllBytes() {
        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
        }
        String hex = DatabaseSecurityManager.bytesToHex(bytes);
        assertEquals(512, hex.length(), "256 bytes should produce 512 hex chars");
    }
}
