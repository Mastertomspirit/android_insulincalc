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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static network.spiritscorp.util.AppConstants.*;

/**
 * Comprehensive unit and integration tests for {@link DatabaseSecurityManager}.
 * Tests hybrid passphrase loading (password-based + legacy), migration support,
 * and password management integration.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class DatabaseSecurityManagerTest {

    private DatabaseSecurityManager securityManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        securityManager = new DatabaseSecurityManager(context);
    }

    @Test
    public void testGetOrCreateDatabasePassphraseReturns32ByteArray() {
        securityManager.setPassword("ThisIsATestPassword123!");
        byte[] passphrase = securityManager.getDatabasePassphrase();
        
        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length);
    }

    @Test
    public void testSetPasswordTwice() {
        String password = "ThisIsATestPassword123!";
        
        boolean setResult = securityManager.setPassword(password);
        assertTrue("Password should be set successfully", setResult);

        boolean setResult2 = securityManager.setPassword(password);
        assertFalse("Password should not be set", setResult2);
    }

    @Test
    public void testIsPasswordSet() {
        assertFalse("Password should not be set initially", securityManager.isPasswordSet());
        securityManager.setPassword("ThisIsATestPassword123!");
        assertTrue("Password should be set after setting", securityManager.isPasswordSet());
    }

    @Test
    public void testMigrationV3Flag() {
        assertFalse("Migration flag should be false initially", securityManager.isMigrationV3Completed());
        securityManager.markMigrationV3Completed();
        assertTrue("Migration flag should be true after marking", securityManager.isMigrationV3Completed());
    }

    @Test
    public void testChangePassword() {
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
}
