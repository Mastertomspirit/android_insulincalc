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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit and component tests for the database encryption layer, AES-256 KeyStore key management,
 * and byte formatting helpers.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class DatabaseSecurityTest {

    @Test
    public void testPassphraseGenerationIs256Bit() {
        Context context = ApplicationProvider.getApplicationContext();
        byte[] passphrase = DatabaseSecurityManager.INSTANCE.getOrCreateDatabasePassphrase(context);

        assertNotNull(passphrase);
        assertEquals(32, passphrase.length); // 32 bytes = 256 bits

        // Subsequent call returns identical persisted passphrase
        byte[] secondPassphrase = DatabaseSecurityManager.INSTANCE.getOrCreateDatabasePassphrase(context);
        assertNotNull(secondPassphrase);
        assertEquals(32, secondPassphrase.length);
        assertTrue(java.util.Arrays.equals(passphrase, secondPassphrase));
    }

    @Test
    public void testBytesToHexFormatting() {
        byte[] bytes = new byte[]{(byte) 0x00, (byte) 0x0F, (byte) 0x1A, (byte) 0xFF};
        String hex = DatabaseSecurityManager.INSTANCE.bytesToHex(bytes);
        assertEquals("000f1aff", hex);
    }

    @Test
    public void testPassphraseHexRepresentationIs64Chars() {
        Context context = ApplicationProvider.getApplicationContext();
        byte[] passphrase = DatabaseSecurityManager.INSTANCE.getOrCreateDatabasePassphrase(context);
        String hex = DatabaseSecurityManager.INSTANCE.bytesToHex(passphrase);
        assertNotNull(hex);
        assertEquals(64, hex.length()); // 32 bytes * 2 hex chars = 64 hex chars
    }

    @Test
    public void testEmptyByteArrayToHex() {
        String hex = DatabaseSecurityManager.INSTANCE.bytesToHex(new byte[0]);
        assertEquals("", hex);
    }
}
