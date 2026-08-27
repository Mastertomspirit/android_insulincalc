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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit and component tests for the database encryption layer, AES-256 key management,
 * plaintext SQLite signature detection, and migration helpers.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34, manifest = Config.NONE)
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
    public void testDetectPlaintextSqliteHeader() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir", "."), "db_test_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try {
            File plaintextFile = new File(tempDir, "sample_plain.db");
            try (FileOutputStream out = new FileOutputStream(plaintextFile)) {
                // Write standard SQLite 3 header: "SQLite format 3\000" + zeroes
                byte[] header = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
                out.write(header);
                out.write(new byte[100]);
            }

            boolean isPlain = DatabaseSecurityManager.INSTANCE.isDatabasePlaintext(plaintextFile);
            assertTrue("Should detect standard SQLite 3 header as plaintext", isPlain);

            File encryptedFile = new File(tempDir, "sample_encrypted.db");
            try (FileOutputStream out = new FileOutputStream(encryptedFile)) {
                // Write random non-SQLite bytes (simulating SQLCipher ciphertext)
                out.write(new byte[]{
                        0x4A, 0x12, (byte) 0x88, 0x7E, (byte) 0x99, 0x01, 0x33, 0x22,
                        0x11, 0x00, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA
                });
            }

            boolean isEncryptedPlain = DatabaseSecurityManager.INSTANCE.isDatabasePlaintext(encryptedFile);
            assertFalse("Encrypted file must NOT be detected as plaintext SQLite", isEncryptedPlain);
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void testEnsureDatabaseEncryptedOnNonExistentFile() {
        Context context = ApplicationProvider.getApplicationContext();
        byte[] passphrase = DatabaseSecurityManager.INSTANCE.getOrCreateDatabasePassphrase(context);
        boolean result = DatabaseSecurityManager.INSTANCE.ensureDatabaseEncrypted(
                context,
                "non_existent_test_db_" + System.currentTimeMillis() + ".db",
                passphrase
        );
        assertTrue("Non-existent database should return true as it will be created fresh encrypted", result);
    }

    private void deleteRecursively(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        fileOrDir.delete();
    }
}
