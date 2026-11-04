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
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_LENGTH, passphrase.length);
    }

    @Test
    void getDatabasePassphraseIsConsistent() {
        byte[] passphrase1 = securityManager.getDatabasePassphrase();
        byte[] passphrase2 = securityManager.getDatabasePassphrase();
        
        assertNotNull(passphrase1);
        assertNotNull(passphrase2);
        assertArrayEquals(passphrase1, passphrase2);
    }

    @Test
    void derivePassphraseReturns32ByteArray() {
        String password = "ThisIsATestPassword1234!";
        byte[] passphrase = securityManager.derivePassphrase(password);
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_LENGTH, passphrase.length);
    }

    @Test
    void derivePassphraseWithTooShortPasswordReturnsNull() {
        String shortPassword = "short";
        byte[] passphrase = securityManager.derivePassphrase(shortPassword);
        
        assertTrue(passphrase == null);
    }

    @Test
    void verifyPasswordWithCorrectPasswordReturnsTrue() {
        String password = "ThisIsATestPassword1234!";
        securityManager.setPassword(password);
        
        boolean result = securityManager.verifyPassword(password);
        assertTrue(result);
    }

    @Test
    void verifyPasswordWithWrongPasswordReturnsFalse() {
        String correctPassword = "ThisIsATestPassword1234!";
        String wrongPassword = "WrongPassword12345678!";
        securityManager.setPassword(correctPassword);
        
        boolean result = securityManager.verifyPassword(wrongPassword);
        assertFalse(result);
    }

    @Test
    void generateRandomPasswordMeetsMinimumLength() {
        String randomPassword = securityManager.generateRandomPassword();
        
        assertNotNull(randomPassword);
        assertTrue(randomPassword.length() >= 20);
    }

    @Test
    void getPasswordManagerReturnsNonNull() {
        PasswordDerivationManager manager = securityManager.getPasswordManager();
        assertNotNull(manager);
    }

    @Test
    void bytesToHexWithEmptyArrayReturnsEmptyString() {
        String hex = DatabaseSecurityManager.bytesToHex(new byte[0]);
        assertEquals("", hex);
    }

    @Test
    void bytesToHexWithNullReturnsEmptyString() {
        String hex = DatabaseSecurityManager.bytesToHex(null);
        assertEquals("", hex);
    }

    @Test
    void bytesToHexWithSampleData() {
        byte[] bytes = new byte[]{(byte) 0x00, (byte) 0x0F, (byte) 0x1A, (byte) 0xFF};
        String hex = DatabaseSecurityManager.bytesToHex(bytes);
        assertEquals("000f1aff", hex);
    }

    @Test
    void bytesToHexWithAllBytes() {
        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
        }
        String hex = DatabaseSecurityManager.bytesToHex(bytes);
        assertEquals(512, hex.length()); // 256 bytes * 2 hex chars = 512 chars
    }
}
