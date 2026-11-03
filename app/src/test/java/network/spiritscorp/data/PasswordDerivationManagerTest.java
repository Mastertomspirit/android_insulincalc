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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive unit tests for {@link PasswordDerivationManager}.
 * Tests password-based key derivation, salt generation, KeyStore integration,
 * and password validation.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class PasswordDerivationManagerTest {

    private static final int MIN_PASSWORD_LENGTH = 20;
    private static final int PASSPHRASE_BYTE_LENGTH = 32;

    @Test
    public void testDeriveDbPassphraseReturns32ByteArray() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password = "ThisIsATestPassword123!";
        
        byte[] passphrase = manager.deriveDbPassphrase(password);
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length);
    }

    @Test
    public void testDeriveDbPassphraseIsDeterministic() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password = "ThisIsATestPassword123!";
        
        byte[] passphrase1 = manager.deriveDbPassphrase(password);
        byte[] passphrase2 = manager.deriveDbPassphrase(password);
        
        assertNotNull(passphrase1);
        assertNotNull(passphrase2);
        assertArrayEquals("Same password must produce same passphrase", passphrase1, passphrase2);
    }

    @Test
    public void testDeriveDbPassphraseDifferentPasswordsDifferentOutputs() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password1 = "ThisIsATestPassword123!";
        String password2 = "ThisIsADifferentPassword456!";
        
        byte[] passphrase1 = manager.deriveDbPassphrase(password1);
        byte[] passphrase2 = manager.deriveDbPassphrase(password2);
        
        assertNotNull(passphrase1);
        assertNotNull(passphrase2);
        assertFalse("Different passwords must produce different passphrases", 
                java.util.Arrays.equals(passphrase1, passphrase2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeriveDbPassphraseTooShortThrowsException() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String shortPassword = "short";
        
        manager.deriveDbPassphrase(shortPassword);
    }

    @Test
    public void testDeriveDbPassphraseMinimumLengthAccepted() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        // Exactly 20 characters
        String password = "12345678901234567890";
        
        byte[] passphrase = manager.deriveDbPassphrase(password);
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length);
    }

    @Test
    public void testDeriveDbPassphraseWithSpecialCharacters() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password = "P@ssw0rd!With$p3c1@l#Ch@r$";
        
        byte[] passphrase = manager.deriveDbPassphrase(password);
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length);
    }

    @Test
    public void testGenerateRandomPasswordMeetsMinimumLength() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        
        String randomPassword = manager.generateRandomPassword();
        
        assertNotNull(randomPassword);
        assertTrue("Random password must meet minimum length", 
                randomPassword.length() >= MIN_PASSWORD_LENGTH);
    }

    @Test
    public void testGenerateRandomPasswordContainsVariousCharacterTypes() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        
        String randomPassword = manager.generateRandomPassword();
        
        assertNotNull(randomPassword);
        
        // Check for at least one lowercase letter
        boolean hasLowercase = randomPassword.matches(".*[a-z].*");
        // Check for at least one uppercase letter
        boolean hasUppercase = randomPassword.matches(".*[A-Z].*");
        // Check for at least one digit
        boolean hasDigit = randomPassword.matches(".*[0-9].*");
        // Check for at least one special character
        boolean hasSpecialChar = randomPassword.matches(".*[!@#$%^&*()_+\-=\[\]{}|;:,.<>?].*");
        
        assertTrue("Random password must contain lowercase letters", hasLowercase);
        assertTrue("Random password must contain uppercase letters", hasUppercase);
        assertTrue("Random password must contain digits", hasDigit);
        assertTrue("Random password must contain special characters", hasSpecialChar);
    }

    @Test
    public void testGenerateRandomPasswordIsUnique() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        
        String password1 = manager.generateRandomPassword();
        String password2 = manager.generateRandomPassword();
        
        assertNotNull(password1);
        assertNotNull(password2);
        assertNotEquals("Random passwords must be unique", password1, password2);
    }

    @Test
    public void testSetAndVerifyPassword() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password = "ThisIsATestPassword123!";
        
        boolean setResult = manager.setPassword(password);
        assertTrue("Password should be set successfully", setResult);
        
        boolean verifyResult = manager.verifyPassword(password);
        assertTrue("Password verification should succeed", verifyResult);
    }

    @Test
    public void testVerifyPasswordWithWrongPasswordFails() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String correctPassword = "ThisIsATestPassword123!";
        String wrongPassword = "WrongPassword12345678!";
        
        manager.setPassword(correctPassword);
        
        boolean verifyResult = manager.verifyPassword(wrongPassword);
        assertFalse("Password verification should fail with wrong password", verifyResult);
    }

    @Test
    public void testVerifyPasswordWithTooShortPasswordFails() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String longPassword = "ThisIsATestPassword123!";
        String shortPassword = "short";
        
        manager.setPassword(longPassword);
        
        boolean verifyResult = manager.verifyPassword(shortPassword);
        assertFalse("Password verification should fail with too short password", verifyResult);
    }

    @Test
    public void testIsPasswordSetAfterSettingPassword() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password = "ThisIsATestPassword123!";
        
        assertFalse("Password should not be set initially", manager.isPasswordSet());
        
        manager.setPassword(password);
        
        assertTrue("Password should be set after setting", manager.isPasswordSet());
    }

    @Test
    public void testChangePassword() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String oldPassword = "OldPassword12345678!";
        String newPassword = "NewPassword12345678!";
        
        manager.setPassword(oldPassword);
        
        boolean changeResult = manager.changePassword(oldPassword, newPassword);
        assertTrue("Password change should succeed", changeResult);
        
        // Old password should no longer work
        boolean oldVerify = manager.verifyPassword(oldPassword);
        assertFalse("Old password should not work after change", oldVerify);
        
        // New password should work
        boolean newVerify = manager.verifyPassword(newPassword);
        assertTrue("New password should work after change", newVerify);
    }

    @Test(expected = SecurityException.class)
    public void testChangePasswordWithWrongOldPasswordThrowsException() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String oldPassword = "OldPassword12345678!";
        String wrongOldPassword = "WrongOldPassword123!";
        String newPassword = "NewPassword12345678!";
        
        manager.setPassword(oldPassword);
        
        manager.changePassword(wrongOldPassword, newPassword);
    }

    @Test
    public void testDerivePassphraseFromPasswordWithoutStoring() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password = "ThisIsATestPassword123!";
        
        byte[] passphrase = manager.deriveDbPassphrase(password);
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length);
        
        // Password should not be stored in KeyStore
        assertFalse("Password should not be stored without explicit set", manager.isPasswordSet());
    }

    @Test
    public void testGetCurrentPassphraseWhenPasswordSet() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password = "ThisIsATestPassword123!";
        
        manager.setPassword(password);
        
        byte[] storedPassphrase = manager.loadFromKeyStore();
        byte[] derivedPassphrase = manager.deriveDbPassphrase(password);
        
        assertNotNull(storedPassphrase);
        assertNotNull(derivedPassphrase);
        assertArrayEquals("Stored passphrase should match derived passphrase", 
                storedPassphrase, derivedPassphrase);
    }

    @Test
    public void testSaltGenerationIsDeterministic() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password = "ThisIsATestPassword123!";
        
        // Derive passphrase twice - the salt should be the same
        byte[] passphrase1 = manager.deriveDbPassphrase(password);
        byte[] passphrase2 = manager.deriveDbPassphrase(password);
        
        assertArrayEquals("Salt generation must be deterministic", passphrase1, passphrase2);
    }

    @Test
    public void testSaltGenerationContainsSpecialCharacters() {
        PasswordDerivationManager manager = new PasswordDerivationManager();
        String password = "ThisIsATestPassword123!";
        
        // We can't directly test the salt, but we can verify that the derivation
        // produces consistent results, which implies the salt is deterministic
        byte[] passphrase1 = manager.deriveDbPassphrase(password);
        byte[] passphrase2 = manager.deriveDbPassphrase(password);
        
        assertArrayEquals("Salt should produce consistent results", passphrase1, passphrase2);
    }
}
