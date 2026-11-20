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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static network.spiritscorp.data.DatabaseConstants.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseSecurityManagerInstrumentedTest {

    private Context context;
    private SharedPreferences preferences;
    private DatabaseSecurityManager manager;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        preferences = context.getSharedPreferences(SECURITY_PREFS_FILE, Context.MODE_PRIVATE );
        clearTestState();
        manager = new DatabaseSecurityManager(context);
    }

    @After
    public void tearDown() throws Exception {
        clearTestState();
    }

    @Test
    public void firstCallCreates32ByteKeyAndPersistsEncryptedMetadata() {
        byte[] key = manager.getOrCreateDatabasePassphrase();

        assertEquals(PASSPHRASE_BYTE_LENGTH, key.length);
        assertTrue(preferences.contains(PASSPHRASE_ENCRYPTED_KEY));
        assertTrue(preferences.contains(PASSPHRASE_IV_KEY));
    }

    @Test
    public void secondCallReturnsTheSameKey() {
        byte[] first = manager.getOrCreateDatabasePassphrase();
        byte[] second = manager.getOrCreateDatabasePassphrase();

        assertArrayEquals(first, second);
    }

    @Test
    public void testNewManagerInstanceCanRecoverSameKeyFromKeystore() {
        byte[] first = manager.getOrCreateDatabasePassphrase();
        DatabaseSecurityManager managerAfterRestart = new DatabaseSecurityManager(context);
        byte[] recovered = managerAfterRestart.getOrCreateDatabasePassphrase();

        assertArrayEquals(first, recovered);
    }

    @Test
    public void testIncompleteStoredMetadataFailsInsteadOfCreatingANewKey() {
        preferences.edit().putString(PASSPHRASE_IV_KEY, "some-iv").commit();

        assertThrows( IllegalStateException.class, () -> manager.getOrCreateDatabasePassphrase() );
        assertFalse(preferences.contains(PASSPHRASE_ENCRYPTED_KEY));
    }

    @Test
    public void testExistingDatabaseWithoutStoredKeyFailsClosed() throws IOException {
        createDatabaseFile();

        assertThrows( IllegalStateException.class, () -> manager.getOrCreateDatabasePassphrase() );
        assertFalse(preferences.contains(PASSPHRASE_ENCRYPTED_KEY));
        assertFalse(preferences.contains(PASSPHRASE_IV_KEY));
    }

    @Test
    public void testExistingDatabaseSidecarWithoutStoredKeyFailsClosed() throws IOException {
        File database = context.getDatabasePath(DATABASE_NAME);
        File parent = database.getParentFile();
        File walFile = new File(database.getAbsolutePath() + "-wal");

        assertTrue(parent == null || parent.exists() || parent.mkdirs());
        assertTrue(walFile.createNewFile());
        assertThrows( IllegalStateException.class, () -> manager.getOrCreateDatabasePassphrase() );
        assertFalse(preferences.contains(PASSPHRASE_ENCRYPTED_KEY));
    }

    @Test
    public void testMissingKeystoreAliasDoesNotReplaceExistingKey() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        manager.getOrCreateDatabasePassphrase();

        String encryptedBefore = preferences.getString(PASSPHRASE_ENCRYPTED_KEY, null);
        String ivBefore = preferences.getString(PASSPHRASE_IV_KEY, null);

        deleteKeystoreAlias();

        assertThrows( IllegalStateException.class, () -> manager.getOrCreateDatabasePassphrase() );
        assertEquals(encryptedBefore, preferences.getString(PASSPHRASE_ENCRYPTED_KEY, null));
        assertEquals(ivBefore, preferences.getString(PASSPHRASE_IV_KEY, null));
        assertFalse(keystoreAliasExists());
    }

    @Test
    public void testModifiedCiphertextFailsAuthenticationAndIsNotReplaced() {
        manager.getOrCreateDatabasePassphrase();

        String encryptedBefore = preferences.getString(PASSPHRASE_ENCRYPTED_KEY, null);
        byte[] ciphertext = Base64.decode( encryptedBefore, Base64.NO_WRAP );

        ciphertext[0] ^= 0x01;

        String tamperedCiphertext = Base64.encodeToString( ciphertext, android.util.Base64.NO_WRAP );

        preferences.edit().putString(PASSPHRASE_ENCRYPTED_KEY, tamperedCiphertext).commit();

        assertThrows( IllegalStateException.class, () -> manager.getOrCreateDatabasePassphrase() );
        assertEquals( tamperedCiphertext, preferences.getString(PASSPHRASE_ENCRYPTED_KEY, null) );
    }

    private void createDatabaseFile() throws IOException {
        File database = context.getDatabasePath(DATABASE_NAME);
        File parent = database.getParentFile();

        assertTrue(parent == null || parent.exists() || parent.mkdirs());
        assertTrue(database.createNewFile());
    }

    private void clearTestState() throws Exception {
        if (preferences != null) {
            preferences.edit().clear().commit();
        }

        deleteKeystoreAlias();

        if (context != null) {
            File database = context.getDatabasePath(DATABASE_NAME);
            String path = database.getAbsolutePath();

            Files.deleteIfExists(Paths.get(path));
            Files.deleteIfExists(Paths.get(path + "-wal"));
            Files.deleteIfExists(Paths.get(path + "-shm"));
            Files.deleteIfExists(Paths.get(path + "-journal"));
        }
    }

    private void deleteKeystoreAlias() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        String alias = SECURITY_KEY_ALIAS;
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias);
        }
    }

    private boolean keystoreAliasExists() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        return keyStore.containsAlias(SECURITY_KEY_ALIAS);
    }
}
