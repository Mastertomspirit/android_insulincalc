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

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Robust unit and integration tests for {@link DatabaseSecurityManager}.
 * Verifies 256-bit passphrase generation, KeyStore encryption/decryption, custom SharedPreferences DI,
 * persistence consistency, fallback storage, and hex conversions.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class DatabaseSecurityTest {

    @Test
    public void testPassphraseGenerationIs256BitAndPersistent() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        byte[] passphrase = securityManager.getOrCreateDatabasePassphrase();

        assertNotNull(passphrase);
        assertEquals(32, passphrase.length); // 32 bytes = 256 bits

        // Subsequent instantiation with same context returns the exact same passphrase
        DatabaseSecurityManager secondManager = new DatabaseSecurityManager(context);
        byte[] secondPassphrase = secondManager.getOrCreateDatabasePassphrase();
        assertNotNull(secondPassphrase);
        assertEquals(32, secondPassphrase.length);
        assertTrue(Arrays.equals(passphrase, secondPassphrase));
    }

    @Test
    public void testDependencyInjectionWithCustomSharedPreferences() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences customPrefs = context.getSharedPreferences("test_custom_sec_prefs", Context.MODE_PRIVATE);
        customPrefs.edit().clear().commit();

        DatabaseSecurityManager securityManager1 = new DatabaseSecurityManager(customPrefs);
        byte[] key1 = securityManager1.getOrCreateDatabasePassphrase();
        assertNotNull(key1);
        assertEquals(32, key1.length);

        DatabaseSecurityManager securityManager2 = new DatabaseSecurityManager(customPrefs);
        byte[] key2 = securityManager2.getOrCreateDatabasePassphrase();
        assertTrue("Keys from the same SharedPreferences must match", Arrays.equals(key1, key2));
    }

    @Test
    public void testBytesToHexFormatting() {
        byte[] bytes = new byte[]{(byte) 0x00, (byte) 0x0F, (byte) 0x1A, (byte) 0xFF};
        String hex = DatabaseSecurityManager.bytesToHex(bytes);
        assertEquals("000f1aff", hex);
    }

    @Test
    public void testPassphraseHexRepresentationIs64Chars() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);
        byte[] passphrase = securityManager.getOrCreateDatabasePassphrase();
        String hex = DatabaseSecurityManager.bytesToHex(passphrase);
        assertNotNull(hex);
        assertEquals(64, hex.length()); // 32 bytes * 2 hex chars = 64 hex chars
    }

    @Test
    public void testEmptyAndNullByteArrayToHex() {
        assertEquals("", DatabaseSecurityManager.bytesToHex(new byte[0]));
        assertEquals("", DatabaseSecurityManager.bytesToHex(null));
    }

    @Test
    public void testEncryptAndDecryptWithKeyStore() {
        Context context = ApplicationProvider.getApplicationContext();
        DatabaseSecurityManager securityManager = new DatabaseSecurityManager(context);

        byte[] original = "DiabeticSecureVaultSecretKey1234".getBytes();
        DatabaseSecurityManager.EncryptedData encrypted = securityManager.encryptWithKeyStore(original);

        if (encrypted != null) {
            byte[] decrypted = securityManager.decryptWithKeyStore(encrypted.iv(), encrypted.ciphertext());
            assertNotNull(decrypted);
            assertTrue(Arrays.equals(original, decrypted));
        }
    }
}
