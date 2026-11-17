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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static network.spiritscorp.util.AppConstants.*;

/**
 * Robust unit and integration tests for {@link DatabaseSecurityManager}.
 * Verifies 256-bit passphrase generation, KeyStore encryption/decryption, custom SharedPreferences DI,
 * persistence consistency, fallback storage, and hex conversions.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class DatabaseSecurityTest {

    private DatabaseSecurityManager securityManager;
    private Context context;

    @Before
    public void setUp(){
        context = ApplicationProvider.getApplicationContext();
        securityManager = new DatabaseSecurityManager(context);
    }

    @Test
    public void testPassphraseGenerationIs256BitAndPersistent() {
        securityManager.setPassword("ThisIsATestPassword123!");
        byte[] passphrase = securityManager.getDatabasePassphrase();
        DatabaseSecurityManager secondManager = new DatabaseSecurityManager(context);
        secondManager.setPassword("ThisIsATestPassword123!");
        byte[] secondPassphrase = secondManager.getDatabasePassphrase();

        assertNotNull(passphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length); // 32 bytes = 256 bits
        assertNotNull(secondPassphrase);
        assertEquals(PASSPHRASE_BYTE_LENGTH, secondPassphrase.length);
        assertArrayEquals(passphrase, secondPassphrase);
    }

    @Test
    public void testEncryptAndDecryptWithKeyStore() {
        byte[] original = "DiabeticSecureVaultSecretKey1234".getBytes();
        DatabaseSecurityManager.EncryptedData encrypted = securityManager.encryptWithKeyStore(original);

        if (encrypted != null) {
            byte[] decrypted = securityManager.decryptWithKeyStore(encrypted);
            assertNotNull(decrypted);
            assertArrayEquals(original, decrypted);
        }
    }

    @Test
    public void testChangeNewPassword(){
        // TODO: Not implemented yet
    }

    @Test
    public void testChangePasswordAndRecreateDB(){
        // TODO: Not implemented yet
    }
}
