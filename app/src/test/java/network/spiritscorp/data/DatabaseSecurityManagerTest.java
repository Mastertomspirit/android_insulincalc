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

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive unit and integration tests for {@link DatabaseSecurityManager}.
 * Tests hybrid passphrase loading (password-based + legacy), migration support,
 * and password management integration.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class DatabaseSecurityManagerTest {

    private static final int PASSPHRASE_BYTE_LENGTH = 32;

    @Test
    public void testGetOrCreateDatabasePassphraseReturns32ByteArray() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        
        byte[] passphrase = securityManager.getOrCreateDatabasePassphrase();
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length);
    }

    @Test
    public void testGetOrCreateDatabasePassphraseIsPersistent() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager1 = new DatabaseSecurityManager(context);
        
        byte[] passphrase1 = securityManager1.getOrCreateDatabasePassphrase();
        
        DatabaseSecurityManager securityManager2 = new DatabaseSecurityManager(context);
        byte[] passphrase2 = securityManager2.getOrCreateDatabasePassphrase();
        
        assertNotNull(passphrase1);
        assertNotNull(passphrase2);
        assertArrayEquals("Passphrase should be persistent across instances", passphrase1, passphrase2);
    }

    @Test
    public void testSetPasswordAndVerify() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        String password = "ThisIsATestPassword123!";
        
        boolean setResult = securityManager.setPassword(password);
        assertTrue("Password should be set successfully", setResult);
        
        boolean verifyResult = securityManager.verifyPassword(password);
        assertTrue("Password verification should succeed", verifyResult);
    }

    @Test
    public void testIsPasswordSet() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        
        assertFalse("Password should not be set initially", securityManager.isPasswordSet());
        
        securityManager.setPassword("ThisIsATestPassword123!");
        
        assertTrue("Password should be set after setting", securityManager.isPasswordSet());
    }

    @Test
    public void testGenerateRandomPassword() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        
        String randomPassword = securityManager.generateRandomPassword();
        
        assertNotNull(randomPassword);
        assertTrue("Random password must meet minimum length", 
                randomPassword.length() >= 20);
    }

    @Test
    public void testGetPasswordManager() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        
        PasswordDerivationManager passwordManager = securityManager.getPasswordManager();
        
        assertNotNull("Password manager should not be null", passwordManager);
    }

    @Test
    public void testDerivePassphraseFromPassword() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        String password = "ThisIsATestPassword123!";
        
        byte[] passphrase = securityManager.derivePassphraseFromPassword(password);
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length);
    }

    @Test
    public void testGetCurrentPassphraseWithPasswordSet() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        String password = "ThisIsATestPassword123!";
        
        securityManager.setPassword(password);
        
        byte[] currentPassphrase = securityManager.getCurrentPassphrase();
        byte[] expectedPassphrase = securityManager.derivePassphraseFromPassword(password);
        
        assertNotNull(currentPassphrase);
        assertNotNull(expectedPassphrase);
        assertArrayEquals("Current passphrase should match derived passphrase", 
                currentPassphrase, expectedPassphrase);
    }

    @Test
    public void testGetCurrentPassphraseWithoutPasswordSet() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        
        // Don't set a password - should use legacy passphrase
        byte[] currentPassphrase = securityManager.getCurrentPassphrase();
        
        assertNotNull(currentPassphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, currentPassphrase.length);
    }

    @Test
    public void testMigrationV3Flag() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("test_migration_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(prefs);
        
        assertFalse("Migration flag should be false initially", 
                securityManager.isMigrationV3Completed());
        
        securityManager.markMigrationV3Completed();
        
        assertTrue("Migration flag should be true after marking", 
                securityManager.isMigrationV3Completed());
    }

    @Test
    public void testChangePassword() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        String oldPassword = "OldPassword12345678!";
        String newPassword = "NewPassword12345678!";
        
        securityManager.setPassword(oldPassword);
        
        boolean changeResult = securityManager.changePassword(oldPassword, newPassword);
        assertTrue("Password change should succeed", changeResult);
        
        // Old password should no longer work
        boolean oldVerify = securityManager.verifyPassword(oldPassword);
        assertFalse("Old password should not work after change", oldVerify);
        
        // New password should work
        boolean newVerify = securityManager.verifyPassword(newPassword);
        assertTrue("New password should work after change", newVerify);
    }

    @Test
    public void testHybridLoadingPasswordFirst() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        
        // Set a password
        String password = "ThisIsATestPassword123!";
        securityManager.setPassword(password);
        
        // Get passphrase - should use password-based
        byte[] passphrase = securityManager.getOrCreateDatabasePassphrase();
        byte[] expected = securityManager.derivePassphraseFromPassword(password);
        
        assertNotNull(passphrase);
        assertNotNull(expected);
        assertArrayEquals("Should use password-based passphrase when available", 
                passphrase, expected);
    }

    @Test
    public void testBytesToHex() {
        byte[] bytes = new byte[]{(byte) 0x00, (byte) 0x0F, (byte) 0x1A, (byte) 0xFF};
        String hex = DatabaseSecurityManager.bytesToHex(bytes);
        assertEquals("000f1aff", hex);
    }

    @Test
    public void testBytesToHexEmptyArray() {
        assertEquals("", DatabaseSecurityManager.bytesToHex(new byte[0]));
    }

    @Test
    public void testBytesToHexNull() {
        assertEquals("", DatabaseSecurityManager.bytesToHex(null));
    }

    @Test
    public void testDependencyInjectionWithCustomSharedPreferences() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences customPrefs = context.getSharedPreferences("test_custom_prefs", Context.MODE_PRIVATE);
        customPrefs.edit().clear().commit();
        
        DatabaseSecurityManager securityManager1 = new DatabaseSecurityManager(customPrefs);
        byte[] key1 = securityManager1.getOrCreateDatabasePassphrase();
        assertNotNull(key1);
        assertEquals(PASSPHRASE_BYTE_LENGTH, key1.length);
        
        DatabaseSecurityManager securityManager2 = new DatabaseSecurityManager(customPrefs);
        byte[] key2 = securityManager2.getOrCreateDatabasePassphrase();
        assertArrayEquals("Keys from the same SharedPreferences must match", key1, key2);
    }
}
