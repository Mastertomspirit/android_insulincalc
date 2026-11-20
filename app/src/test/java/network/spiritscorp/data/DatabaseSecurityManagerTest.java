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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import static network.spiritscorp.data.DatabaseConstants.*;

/**
 * Unit tests for {@link DatabaseSecurityManager}.
 * <p>
 * Runs under Robolectric (JUnit 4 runner) because the class under test needs the real
 * android.util.Base64, KeyGenParameterSpec and Log. The Android KeyStore itself is replaced by
 * {@link FakeAndroidKeyStore}, a real JCA provider named "AndroidKeyStore", so the production code
 * runs completely unchanged and real AES-GCM encryption/decryption takes place.
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DatabaseSecurityManagerTest {

    // ---- specification constants  ----
    private static final int TOO_SHORT_PASSPHRASE_BYTES = PASSPHRASE_BYTE_LENGTH - 1;
    private static final int TOO_LONG_PASSPHRASE_BYTES = PASSPHRASE_BYTE_LENGTH + 1;
    private static final int KEY_SIZE_BITS = 256;
    private static final int GCM_TAG_BYTES = GCM_TAG_LENGTH_BITS / 8;

    // ---- test data ----
    private static final String EMPTY_VALUE = "";
    private static final String BLANK_VALUE = "   ";
    private static final String INVALID_BASE64 = "A";
    private static final String VALID_BASE64 = "AQID"; // Base64 of {1, 2, 3}
    private static final String SUFFIX_DATABASE = "";
    private static final String SUFFIX_WAL = "-wal";
    private static final String SUFFIX_SHM = "-shm";
    private static final String LOG_TAG = DatabaseSecurityManager.class.getSimpleName();

    // Fragments of the production exception messages.
    private static final String MSG_INCOMPLETE = "incomplete";
    private static final String MSG_NOT_RECOVERED = "could not be recovered";
    private static final String MSG_INVALID_LENGTH = "invalid length";
    private static final String MSG_DB_WITHOUT_KEY = "stored encryption key is missing";
    private static final String MSG_KEYSTORE_KEY_MISSING = "Android Keystore key is missing";
    private static final String MSG_KEYSTORE_NOT_ACCESSIBLE = "could not be accessed";
    private static final String MSG_EXISTING_KEY_UNAVAILABLE = "existing Android Keystore key is unavailable";
    private static final String MSG_DECRYPTION_UNAVAILABLE = "decryption not available";
    private static final String MSG_CREATE_FAILED = "Could not securely create";
    private static final String MSG_NOT_PERSISTED = "Could not persist";

    private Context context;
    private SharedPreferences prefs;
    private FakeAndroidKeyStore fakeKeyStore;
    private DatabaseSecurityManager manager;
    private SharedPreferences.Editor mockEditor;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(SECURITY_PREFS_FILE, Context.MODE_PRIVATE);
        fakeKeyStore = new FakeAndroidKeyStore();
        fakeKeyStore.install();
        manager = new DatabaseSecurityManager(context);
    }

    @After
    public void tearDown() {
        fakeKeyStore.uninstall();
    }

    // =====================================================================================
    // First run: key and passphrase creation
    // =====================================================================================

    @Test
    public void firstRun_returnsPassphraseOf32Bytes() {
        assertEquals(PASSPHRASE_BYTE_LENGTH, manager.getOrCreateDatabasePassphrase().length);
    }

    @Test
    public void firstRun_differentInstallationsGetDifferentPassphrases() {
        byte[] first = manager.getOrCreateDatabasePassphrase();
        assertTrue(prefs.edit().clear().commit());

        byte[] second = manager.getOrCreateDatabasePassphrase();

        assertFalse(Arrays.equals(first, second));
    }

    @Test
    public void firstRun_persistsEncryptedValueAndIv() {
        manager.getOrCreateDatabasePassphrase();

        assertAll(
                () -> assertTrue(prefs.contains(PASSPHRASE_ENCRYPTED_KEY)),
                () -> assertTrue(prefs.contains(PASSPHRASE_IV_KEY)));
    }

    @Test
    public void firstRun_storedValueIsNotThePlaintextPassphrase() {
        byte[] passphrase = manager.getOrCreateDatabasePassphrase();

        assertFalse(Arrays.equals(passphrase, storedBytes(PASSPHRASE_ENCRYPTED_KEY)));
    }

    @Test
    public void firstRun_storedCiphertextIsPassphraseLengthPlusGcmTag() {
        manager.getOrCreateDatabasePassphrase();

        assertEquals(PASSPHRASE_BYTE_LENGTH + GCM_TAG_BYTES, storedBytes(PASSPHRASE_ENCRYPTED_KEY).length);
    }

    @Test
    public void firstRun_storedValueDecryptsWithKeystoreKeyToReturnedPassphrase() throws Exception {
        byte[] passphrase = manager.getOrCreateDatabasePassphrase();

        assertArrayEquals(passphrase, decryptStored(fakeKeyStore.getKey(SECURITY_KEY_ALIAS)));
    }

    @Test
    public void firstRun_createsKeystoreKeyUnderConfiguredAlias() {
        manager.getOrCreateDatabasePassphrase();

        assertTrue(fakeKeyStore.containsAlias(SECURITY_KEY_ALIAS));
    }

    @Test
    public void firstRun_keySpecIsAes256GcmWithoutPaddingAndRandomizedIv() {
        manager.getOrCreateDatabasePassphrase();
        KeyGenParameterSpec spec = fakeKeyStore.getGenerationRequests().getFirst();

        assertAll(
                () -> assertEquals(SECURITY_KEY_ALIAS, spec.getKeystoreAlias()),
                () -> assertEquals(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT, spec.getPurposes()),
                () -> assertArrayEquals(new String[]{KeyProperties.BLOCK_MODE_GCM}, spec.getBlockModes()),
                () -> assertArrayEquals(new String[]{KeyProperties.ENCRYPTION_PADDING_NONE}, spec.getEncryptionPaddings()),
                () -> assertEquals(KEY_SIZE_BITS, spec.getKeySize()),
                () -> assertTrue(spec.isRandomizedEncryptionRequired()));
    }

    @Test
    public void firstRun_triesStrongBoxFirst() {
        manager.getOrCreateDatabasePassphrase();
        List<KeyGenParameterSpec> requests = fakeKeyStore.getGenerationRequests();

        assertAll(
                () -> assertEquals(1, requests.size()),
                () -> assertTrue(requests.getFirst().isStrongBoxBacked()));
    }

    @Test
    public void firstRun_fallsBackToKeyWithoutStrongBoxWhenStrongBoxIsUnavailable() {
        fakeKeyStore.strongBoxUnavailable = true;

        byte[] passphrase = manager.getOrCreateDatabasePassphrase();
        List<KeyGenParameterSpec> requests = fakeKeyStore.getGenerationRequests();

        assertAll(
                () -> assertEquals(2, requests.size()),
                () -> assertTrue(requests.getFirst().isStrongBoxBacked()),
                () -> assertFalse(requests.get(1).isStrongBoxBacked()),
                () -> assertTrue(fakeKeyStore.containsAlias(SECURITY_KEY_ALIAS)),
                () -> assertEquals(PASSPHRASE_BYTE_LENGTH, passphrase.length));
    }

    @Test
    public void firstRun_reusesExistingKeystoreKeyInsteadOfGeneratingANewOne() throws Exception {
        SecretKey existingKey = fakeKeyStore.putKey(SECURITY_KEY_ALIAS);

        byte[] passphrase = manager.getOrCreateDatabasePassphrase();

        assertAll(
                () -> assertTrue(fakeKeyStore.getGenerationRequests().isEmpty()),
                () -> assertArrayEquals(passphrase, decryptStored(existingKey)));
    }

    // =====================================================================================
    // Subsequent runs: reading the stored passphrase
    // =====================================================================================

    @Test
    public void secondCall_returnsSamePassphrase() {
        byte[] first = manager.getOrCreateDatabasePassphrase();

        assertArrayEquals(first, manager.getOrCreateDatabasePassphrase());
    }

    @Test
    public void newManagerInstance_returnsSamePassphrase() {
        byte[] first = manager.getOrCreateDatabasePassphrase();

        assertArrayEquals(first, new DatabaseSecurityManager(context).getOrCreateDatabasePassphrase());
    }

    @Test
    public void secondCall_doesNotGenerateAnotherKey() {
        manager.getOrCreateDatabasePassphrase();
        manager.getOrCreateDatabasePassphrase();

        assertEquals(1, fakeKeyStore.getGenerationRequests().size());
    }

    @Test
    public void secondCall_doesNotChangeStoredValues() {
        manager.getOrCreateDatabasePassphrase();
        Map<String, ?> before = new HashMap<>(prefs.getAll());

        manager.getOrCreateDatabasePassphrase();

        assertEquals(before, prefs.getAll());
    }

    @Test
    public void databaseExists_withStoredKey_returnsPassphrase() throws Exception {
        byte[] first = manager.getOrCreateDatabasePassphrase();
        createDatabaseArtifact(SUFFIX_DATABASE);

        assertArrayEquals(first, manager.getOrCreateDatabasePassphrase());
    }

    // =====================================================================================
    // Incomplete or corrupt stored data
    // =====================================================================================

    @Test
    public void incompleteData_onlyEncryptedValue_fails() {
        storeRaw(VALID_BASE64, null);

        assertMessageContains(callAndCatch(), MSG_INCOMPLETE);
    }

    @Test
    public void incompleteData_onlyIv_fails() {
        storeRaw(null, VALID_BASE64);

        assertMessageContains(callAndCatch(), MSG_INCOMPLETE);
    }

    @Test
    public void incompleteData_emptyEncryptedValue_fails() {
        storeRaw(EMPTY_VALUE, VALID_BASE64);

        assertMessageContains(callAndCatch(), MSG_INCOMPLETE);
    }

    @Test
    public void incompleteData_blankEncryptedValue_fails() {
        storeRaw(BLANK_VALUE, VALID_BASE64);

        assertMessageContains(callAndCatch(), MSG_INCOMPLETE);
    }

    @Test
    public void incompleteData_emptyIv_fails() {
        storeRaw(VALID_BASE64, EMPTY_VALUE);

        assertMessageContains(callAndCatch(), MSG_INCOMPLETE);
    }

    @Test
    public void incompleteData_blankIv_fails() {
        storeRaw(VALID_BASE64, BLANK_VALUE);

        assertMessageContains(callAndCatch(), MSG_INCOMPLETE);
    }

    @Test
    public void incompleteData_neverOverwritesStoredValuesOrCreatesAKey() {
        storeRaw(null, VALID_BASE64);
        Map<String, ?> before = new HashMap<>(prefs.getAll());

        callAndCatch();

        assertAll(
                () -> assertEquals(before, prefs.getAll()),
                () -> assertTrue(fakeKeyStore.getGenerationRequests().isEmpty()));
    }

    @Test
    public void invalidBase64InEncryptedValue_failsWithCause() {
        storeRaw(INVALID_BASE64, VALID_BASE64);
        IllegalStateException e = callAndCatch();

        assertAll(
                () -> assertMessageContains(e, MSG_NOT_RECOVERED),
                () -> assertInstanceOf(IllegalArgumentException.class, e.getCause()));
    }

    @Test
    public void invalidBase64InIv_failsWithCause() {
        storeRaw(VALID_BASE64, INVALID_BASE64);
        IllegalStateException e = callAndCatch();

        assertAll(
                () -> assertMessageContains(e, MSG_NOT_RECOVERED),
                () -> assertInstanceOf(IllegalArgumentException.class, e.getCause()));
    }

    @Test
    public void keyMissingInKeystore_failsAndDoesNotCreateANewKey() {
        manager.getOrCreateDatabasePassphrase();
        fakeKeyStore.removeKey(SECURITY_KEY_ALIAS); // e.g. key lost after a restore from backup
        int requestsBefore = fakeKeyStore.getGenerationRequests().size();

        IllegalStateException e = callAndCatch();

        assertAll(
                () -> assertMessageContains(e, MSG_KEYSTORE_KEY_MISSING),
                () -> assertEquals(requestsBefore, fakeKeyStore.getGenerationRequests().size()),
                () -> assertFalse(fakeKeyStore.containsAlias(SECURITY_KEY_ALIAS)));
    }

    @Test
    public void keystoreKeyReplaced_failsBecauseStoredValueCannotBeDecrypted() {
        manager.getOrCreateDatabasePassphrase();
        fakeKeyStore.putKey(SECURITY_KEY_ALIAS); // overwrites the key with a different one

        assertMessageContains(callAndCatch(), MSG_DECRYPTION_UNAVAILABLE);
    }

    @Test
    public void tamperedCiphertext_failsBecauseGcmTagDoesNotMatch() {
        manager.getOrCreateDatabasePassphrase();
        tamperStoredCiphertext();

        assertMessageContains(callAndCatch(), MSG_DECRYPTION_UNAVAILABLE);
    }

    @Test
    public void decryptionFailure_isLoggedAsError() {
        manager.getOrCreateDatabasePassphrase();
        tamperStoredCiphertext();

        callAndCatch();

        assertTrue(ShadowLog.getLogsForTag(LOG_TAG).stream().anyMatch(item -> item.type == Log.ERROR));
    }

    @Test
    public void storedPassphraseTooShort_fails() throws Exception {
        storeEncrypted(fakeKeyStore.putKey(SECURITY_KEY_ALIAS), randomBytes(TOO_SHORT_PASSPHRASE_BYTES));

        assertMessageContains(callAndCatch(), MSG_INVALID_LENGTH);
    }

    @Test
    public void storedPassphraseTooLong_fails() throws Exception {
        storeEncrypted(fakeKeyStore.putKey(SECURITY_KEY_ALIAS), randomBytes(TOO_LONG_PASSPHRASE_BYTES));

        assertMessageContains(callAndCatch(), MSG_INVALID_LENGTH);
    }

    // =====================================================================================
    // Database exists but there is no stored key
    // =====================================================================================

    @Test
    public void databaseFileExists_withoutStoredKey_fails() throws Exception {
        createDatabaseArtifact(SUFFIX_DATABASE);

        assertMessageContains(callAndCatch(), MSG_DB_WITHOUT_KEY);
    }

    @Test
    public void walFileExists_withoutStoredKey_fails() throws Exception {
        createDatabaseArtifact(SUFFIX_WAL);

        assertMessageContains(callAndCatch(), MSG_DB_WITHOUT_KEY);
    }

    @Test
    public void shmFileExists_withoutStoredKey_fails() throws Exception {
        createDatabaseArtifact(SUFFIX_SHM);

        assertMessageContains(callAndCatch(), MSG_DB_WITHOUT_KEY);
    }

    @Test
    public void databaseExists_withoutStoredKey_neverCreatesAKeyOrStoresAnything() throws Exception {
        createDatabaseArtifact(SUFFIX_DATABASE);

        callAndCatch();

        assertAll(
                () -> assertTrue(prefs.getAll().isEmpty()),
                () -> assertTrue(fakeKeyStore.getGenerationRequests().isEmpty()));
    }

    // =====================================================================================
    // KeyStore failures
    // =====================================================================================

    @Test
    public void keystoreProviderMissing_fails() {
        fakeKeyStore.uninstall(); // like a plain JVM without AndroidKeyStore

        assertMessageContains(callAndCatch(), MSG_KEYSTORE_NOT_ACCESSIBLE);
    }

    @Test
    public void keystoreCannotBeLoaded_fails() {
        fakeKeyStore.loadFailure = new IOException();

        assertMessageContains(callAndCatch(), MSG_KEYSTORE_NOT_ACCESSIBLE);
    }

    @Test
    public void existingAliasWithNullEntry_fails() {
        fakeKeyStore.putKey(SECURITY_KEY_ALIAS);
        fakeKeyStore.returnNullEntry = true;

        assertMessageContains(callAndCatch(), MSG_EXISTING_KEY_UNAVAILABLE);
    }

    @Test
    public void existingAliasWithUnrecoverableEntry_fails() {
        fakeKeyStore.putKey(SECURITY_KEY_ALIAS);
        fakeKeyStore.entryFailure = new UnrecoverableEntryException();

        assertMessageContains(callAndCatch(), MSG_KEYSTORE_NOT_ACCESSIBLE);
    }

    @Test
    public void keyGeneratorRejectsSpec_failsWithCauseAndStoresNothing() {
        InvalidAlgorithmParameterException failure = new InvalidAlgorithmParameterException();
        fakeKeyStore.generatorFailure = failure;

        IllegalStateException e = callAndCatch();

        assertAll(
                () -> assertMessageContains(e, MSG_CREATE_FAILED),
                () -> assertSame(failure, e.getCause()),
                () -> assertTrue(prefs.getAll().isEmpty()));
    }

    // =====================================================================================
    // SharedPreferences behavior (needs a mocked Context to force commit() == false)
    // =====================================================================================

    @Test
    public void firstRun_commitFails_throws() {
        DatabaseSecurityManager failing = new DatabaseSecurityManager(contextWithMockedPrefs(false));

        IllegalStateException e = assertThrows(IllegalStateException.class, failing::getOrCreateDatabasePassphrase);

        assertMessageContains(e, MSG_NOT_PERSISTED);
    }

    @Test
    public void firstRun_persistsSynchronouslyWithCommitNotApply() {
        new DatabaseSecurityManager(contextWithMockedPrefs(true)).getOrCreateDatabasePassphrase();

        assertAll(
                () -> verify(mockEditor).commit(),
                () -> verify(mockEditor, never()).apply());
    }

    @Test
    public void constructor_readsPreferencesFromApplicationContext() {
        Context wrapper = mock(Context.class);
        when(wrapper.getApplicationContext()).thenReturn(context);
        when(wrapper.getDatabasePath(DATABASE_NAME)).thenReturn(context.getDatabasePath(DATABASE_NAME));

        new DatabaseSecurityManager(wrapper).getOrCreateDatabasePassphrase();

        assertAll(
                () -> assertTrue(prefs.contains(PASSPHRASE_ENCRYPTED_KEY)),
                () -> verify(wrapper, never()).getSharedPreferences(anyString(), anyInt()));
    }

    // =====================================================================================
    // Fixtures / helpers (they only arrange test data; every assertion targets production behavior)
    // =====================================================================================

    private IllegalStateException callAndCatch() {
        return assertThrows(IllegalStateException.class, () -> manager.getOrCreateDatabasePassphrase());
    }

    private static void assertMessageContains(IllegalStateException e, String fragment) {
        assertTrue(Objects.requireNonNull(e.getMessage()).contains(fragment), e.getMessage());
    }

    private void storeRaw(String encryptedValue, String ivValue) {
        SharedPreferences.Editor editor = prefs.edit();
        if (encryptedValue != null) {
            editor.putString(PASSPHRASE_ENCRYPTED_KEY, encryptedValue);
        }
        if (ivValue != null) {
            editor.putString(PASSPHRASE_IV_KEY, ivValue);
        }
        assertTrue(editor.commit());
    }

    /** Stores {@code plaintext} encrypted with {@code key} in exactly the format the production code expects. */
    private void storeEncrypted(SecretKey key, byte[] plaintext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plaintext);
        storeRaw(encode(encrypted), encode(iv));
    }

    /** Independently decrypts the currently stored value, so tests can verify what was really persisted. */
    private byte[] decryptStored(SecretKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, storedBytes(PASSPHRASE_IV_KEY)));
        return cipher.doFinal(storedBytes(PASSPHRASE_ENCRYPTED_KEY));
    }

    private void tamperStoredCiphertext() {
        byte[] encrypted = storedBytes(PASSPHRASE_ENCRYPTED_KEY);
        encrypted[0] ^= 0x01;
        assertTrue(prefs.edit().putString(PASSPHRASE_ENCRYPTED_KEY, encode(encrypted)).commit());
    }

    private byte[] storedBytes(String prefKey) {
        return Base64.decode(prefs.getString(prefKey, null), Base64.NO_WRAP);
    }

    private static String encode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private void createDatabaseArtifact(String suffix) throws IOException {
        File database = context.getDatabasePath(DATABASE_NAME);
        File directory = database.getParentFile();
        Assert.assertNotNull(directory);
        assertTrue(directory.exists() || directory.mkdirs());
        assertTrue(new File(database.getAbsolutePath() + suffix).createNewFile());
    }

    /** A Context whose SharedPreferences are mocks, so the result of commit() can be controlled. */
    private Context contextWithMockedPrefs(boolean commitSucceeds) {
        Context mockContext = mock(Context.class);
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        mockEditor = mock(SharedPreferences.Editor.class);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getSharedPreferences(SECURITY_PREFS_FILE, Context.MODE_PRIVATE)).thenReturn(mockPrefs);
        when(mockContext.getDatabasePath(DATABASE_NAME)).thenReturn(context.getDatabasePath(DATABASE_NAME));
        when(mockPrefs.getString(anyString(), isNull())).thenReturn(null);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.commit()).thenReturn(commitSucceeds);
        return mockContext;
    }
}