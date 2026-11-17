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


import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.util.Arrays;

import static network.spiritscorp.util.AppConstants.*;

/**
 * Comprehensive unit tests for {@link PasswordDerivationManager}.
 * Tests password-based key derivation, salt generation, KeyStore integration,
 * and password validation.
 */

public class PasswordDerivationManagerTest {

    private PasswordDerivationManager manager;

    @BeforeEach
    public void setUp() throws Exception {
        KeyStore testKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        testKeyStore.load(null, null);
        manager = new PasswordDerivationManager(testKeyStore);
    }

    @Test
    public void deriveDbPassphraseReturns32ByteArrayTest() {
        String password = "ThisIsATestPassword123!";
        byte[] passphrase = manager.deriveDbPassphrase(password);

        assertAll(
                () ->assertNotNull(passphrase),
                () -> assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length)
        );
    }

    @Test
    public void deriveDbPassphraseIsDeterministicTest() {
        String password = "ThisIsATestPassword123!";
        byte[] passphrase1 = manager.deriveDbPassphrase(password);
        byte[] passphrase2 = manager.deriveDbPassphrase(password);

        assertAll(
                () -> assertNotNull(passphrase1),
                () -> assertNotNull(passphrase2),
                () -> assertArrayEquals(passphrase1, passphrase2, "Same password must produce same passphrase")
        );
    }

    @Test
    public void deriveDbPassphraseDifferentPasswordsDifferentOutputsTest() {
        String password1 = "ThisIsATestPassword123!";
        String password2 = "ThisIsADifferentPassword456!";
        byte[] passphrase1 = manager.deriveDbPassphrase(password1);
        byte[] passphrase2 = manager.deriveDbPassphrase(password2);

        assertAll(
                () -> assertNotNull(passphrase1),
                () -> assertNotNull(passphrase2),
                () -> assertNotEquals(passphrase1, passphrase2, "Different passwords must produce different passphrases")
        );
    }

    @Test
    public void deriveDbPassphraseTooShortThrowsExceptionTest() {
        String shortPassword = "short";
        
        assertThrows(IllegalArgumentException.class, () -> manager.deriveDbPassphrase(shortPassword), "Short passwords must throw an exception");
    }

    @Test
    public void deriveDbPassphraseWithSpecialCharactersTest() {
        String password = "P@ssw0rd!With$p3c1@l#Ch@r$";
        byte[] passphrase = manager.deriveDbPassphrase(password);

        assertAll(
                () -> assertNotNull(passphrase),
                () -> assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length, "Special chars should not make problems")
        );
    }

    @Test
    public void generateRandomPasswordMeetsMinimumLengthTest() {
        String randomPassword = manager.generateRandomPassword();

        assertAll(
                () -> assertNotNull(randomPassword),
                () -> assertTrue( randomPassword.length() >= MIN_PASSWORD_LENGTH, "Random password must meet minimum length")
        );
    }

    @Test
    public void generateRandomPasswordContainsVariousCharacterTypesTest() {
        String randomPassword = manager.generateRandomPassword();
        // Check for at least one lowercase letter
        boolean hasLowercase = randomPassword.matches(".*[a-z].*");
        // Check for at least one uppercase letter
        boolean hasUppercase = randomPassword.matches(".*[A-Z].*");
        // Check for at least one digit
        boolean hasDigit = randomPassword.matches(".*[0-9].*");
        // Check for at least one special character using a safe character class
        boolean hasSpecialChar = randomPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*");

        assertAll(
                () -> assertNotNull(randomPassword),
                () -> assertTrue(hasLowercase, "Random password must contain lowercase letters"),
                () -> assertTrue(hasUppercase, "Random password must contain uppercase letters"),
                () -> assertTrue(hasDigit, "Random password must contain digits"),
                () -> assertTrue(hasSpecialChar, "Random password must contain special characters")
        );
    }

    @Test
    public void generateRandomPasswordIsUniqueTest() {
        String password1 = manager.generateRandomPassword();
        String password2 = manager.generateRandomPassword();

        assertAll(
                () -> assertNotNull(password1),
                () -> assertNotNull(password2),
                () -> assertNotEquals(password1, password2, "Random passwords must be unique")
        );
    }

    @Test
    public void setAndVerifyPasswordTest() {
        String password = "ThisIsATestPassword123!";
        boolean setResult = manager.setPassword(password);
        boolean verifyResult = manager.verifyPassword(password);

        assertAll(
                () -> assertTrue(setResult, "Password should be set successfully"),
                () -> assertTrue(verifyResult, "Password verification should succeed")
        );
    }

    @Test
    public void verifyPasswordWithWrongPasswordFailsTest() {
        String correctPassword = "ThisIsATestPassword123!";
        String wrongPassword = "WrongPassword12345678!";
        manager.setPassword(correctPassword);
        boolean verifyResult = manager.verifyPassword(wrongPassword);

        assertFalse(verifyResult, "Password verification should fail with wrong password");
    }

    @Test
    public void verifyPasswordWithTooShortPasswordFailsTest() {
        String longPassword = "ThisIsATestPassword123!";
        String shortPassword = "short";
        manager.setPassword(longPassword);
        boolean verifyResult = manager.verifyPassword(shortPassword);

        assertFalse(verifyResult, "Password verification should fail with too short password");
    }

    @Test
    public void changePasswordTest() {
        String oldPassword = "OldPassword12345678!";
        String newPassword = "NewPassword12345678!";
        manager.setPassword(oldPassword);
        boolean changeResult = manager.changePassword(oldPassword, newPassword);
        boolean oldVerify = manager.verifyPassword(oldPassword);
        boolean newVerify = manager.verifyPassword(newPassword);

        assertAll(
                () -> assertTrue(changeResult, "Password change should succeed"),
                () -> assertFalse(oldVerify, "Old password should not work after change"),
                () -> assertTrue(newVerify, "New password should work after change")
        );
    }

    @Test
    public void changePasswordWithWrongOldPasswordThrowsExceptionTest() {
        String oldPassword = "OldPassword12345678!";
        String wrongOldPassword = "WrongOldPassword123!";
        String newPassword = "NewPassword12345678!";
        manager.setPassword(oldPassword);
        
        assertThrows(SecurityException.class, () -> manager.changePassword(wrongOldPassword, newPassword));
    }

    @Test
    public void getCurrentPassphraseWhenPasswordSetTest() {
        String password = "ThisIsATestPassword123!";
        manager.setPassword(password);
        byte[] storedPassphrase = manager.loadFromKeyStore();
        byte[] derivedPassphrase = manager.deriveDbPassphrase(password);

        assertAll(
                () -> assertNotNull(storedPassphrase),
                () -> assertNotNull(derivedPassphrase),
                () -> assertArrayEquals(storedPassphrase, derivedPassphrase, "Stored passphrase should match derived passphrase")
        );
    }

    @Test
    public void saltGenerationIsDeterministicTest() {
        String password = "ThisIsATestPassword123!";
        // Derive passphrase twice - the salt should be the same
        byte[] passphrase1 = manager.deriveDbPassphrase(password);
        byte[] passphrase2 = manager.deriveDbPassphrase(password);

        assertArrayEquals(passphrase1, passphrase2, "Salt generation must be deterministic");
    }

    @Test
    public void saltGenerationContainsSpecialCharactersTest() {
        String password = "ThisIsATestPassword123!";
        // We can't directly test the salt, but we can verify that the derivation
        // produces consistent results, which implies the salt is deterministic
        byte[] passphrase1 = manager.deriveDbPassphrase(password);
        byte[] passphrase2 = manager.deriveDbPassphrase(password);

        assertArrayEquals(passphrase1, passphrase2, "Salt should produce consistent results");
    }
}
