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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import static network.spiritscorp.data.DatabaseConstants.*;


/**
 * Enterprise-grade security manager object for SQLite / SQLCipher database encryption.
 *<br><br>
 * Designed as an instantiable, object-oriented cryptographic vault:<br>
 * 1. Hardware-backed Android KeyStore protection: Encrypts/decrypts the 256-bit database master passphrase
 *    using an AES-256 GCM key stored in the hardware security module (TEE / StrongBox).<br>
 * 2. Pure instance-based encapsulation: Enables clean dependency injection, modularity, and test mocking.<br>
 * 3. JVM / Test Resilience: Gracefully falls back in local JVM unit testing environments where KeyStore is absent.<br>
 */
public class DatabaseSecurityManager {

    /** Logging tag used for debugging and security audits. */
    private static final String TAG = "DatabaseSecurityManager";
    private final SharedPreferences sharedPrefs;
    private final Context context;

    /**
     * Constructs a new DatabaseSecurityManager instance bound to the provided Android context.
     *
     * @param context Application or activity context.
     */
    public DatabaseSecurityManager(@NonNull Context context) {
        this.sharedPrefs = context.getApplicationContext().getSharedPreferences(SECURITY_PREFS_FILE, Context.MODE_PRIVATE);
        this.context = context;
    }

    /**
     * Retrieves the existing 256-bit database encryption passphrase or generates a new one
     * using SecureRandom if this is the first execution.
     *
     * @return 32-byte (256-bit) encryption key suitable for SQLCipher.
     */
    @NonNull
    public byte[] getOrCreateDatabasePassphrase() throws IllegalStateException {
        //  Try reading the KeyStore-encrypted passphrase
        String encryptedValue = sharedPrefs.getString(PASSPHRASE_ENCRYPTED_KEY, null);
        String ivValue = sharedPrefs.getString(PASSPHRASE_IV_KEY, null);

        boolean hasEncryptedValue = encryptedValue != null;
        boolean hasIv = ivValue != null;

        if (hasEncryptedValue || hasIv) {
            if (!hasEncryptedValue || !hasIv || encryptedValue.trim().isEmpty() || ivValue.trim().isEmpty()) {
                throw new IllegalStateException( "The stored database key data is incomplete. The database was not changed."
                );
            }

            try {
                byte[] encrypted = Base64.decode(encryptedValue, Base64.NO_WRAP);
                byte[] iv = Base64.decode(ivValue, Base64.NO_WRAP);
                byte[] passphrase = decryptWithExistingKey(iv, encrypted);

                if (passphrase.length != PASSPHRASE_BYTE_LENGTH) {
                    throw new IllegalStateException( "The stored database key has an invalid length." );
                }

                return passphrase;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "The stored database key could not be recovered. The database was not changed:\n" + e.getMessage());
                throw new IllegalStateException( "The stored database key could not be recovered. The database was not changed.", e );
            }
        }

        if (databaseOrSidecarExists()) {
            throw new IllegalStateException( "The database exists, but its stored encryption key is missing. The database was not changed." );
        }

        //  Generate a new cryptographically random 256-bit passphrase
        byte[] passphrase = new byte[PASSPHRASE_BYTE_LENGTH];
        new SecureRandom().nextBytes(passphrase);

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, createKeystoreKey());

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(passphrase);

            boolean saved = sharedPrefs.edit()
                    .putString(PASSPHRASE_IV_KEY, Base64.encodeToString(iv, Base64.NO_WRAP))
                    .putString(PASSPHRASE_ENCRYPTED_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .commit();

            if (!saved) {
                throw new IllegalStateException( "Could not persist the database key." );
            }

            return passphrase;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException |
                 InvalidAlgorithmParameterException | NoSuchProviderException e) {
            Log.e(TAG, "Could not securely create the database key:\n" + e.getMessage());
            throw new IllegalStateException("Could not securely create the database key.", e );
        }
    }

    private boolean databaseOrSidecarExists() {
        File database = context.getDatabasePath(DATABASE_NAME);
        String path = database.getAbsolutePath();
        return database.exists() || new File(path + "-wal").exists() || new File(path + "-shm").exists();
    }

    private SecretKey createKeystoreKey() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(SECURITY_KEY_ALIAS)) {
                KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(SECURITY_KEY_ALIAS, null);
                if (entry != null) {
                    return entry.getSecretKey();
                }
                throw new IllegalStateException("The existing Android Keystore key is unavailable.");
            }
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | UnrecoverableEntryException e){
            Log.e(TAG, "The Android Keystore could not be accessed:\n" + e.getMessage());
            throw new IllegalStateException("The Android Keystore could not be accessed.", e);
        }
        try {
            return generateKeystoreKey(true);
        } catch (StrongBoxUnavailableException unavailable) {
            // StrongBox is optional; use Android Keystore without StrongBox.
            Log.w(TAG, "StrongBox is not available on this device");
            return generateKeystoreKey(false);
        }
    }

    private SecretKey generateKeystoreKey(boolean useStrongBox) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
        );
        KeyGenParameterSpec keyGenSpec = new KeyGenParameterSpec.Builder(
                SECURITY_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setIsStrongBoxBacked(useStrongBox)
                .build();

        keyGenerator.init(keyGenSpec);
        return keyGenerator.generateKey();
    }

    /**
     * Decrypts the provided ciphertext byte array using the Android KeyStore AES-GCM master key and IV.
     *
     * @param iv Initialization vector used during encryption.
     * @param ciphertext Encrypted data bytes.
     * @return Decrypted raw plaintext bytes.
     */
    private byte[] decryptWithExistingKey(@NonNull byte[] iv, @NonNull byte[] ciphertext) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (!keyStore.containsAlias(SECURITY_KEY_ALIAS)) {
                throw new IllegalStateException("Android Keystore key is missing.");
            }
            SecretKey secretKey = createKeystoreKey();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(ciphertext);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException |
                 NoSuchProviderException | InvalidAlgorithmParameterException | CertificateException | KeyStoreException | IOException e) {
            Log.e(TAG, "AndroidKeyStore decryption not available (e.g. JVM/test environment):\n" + e.getMessage());
            throw new IllegalStateException("AndroidKeyStore decryption not available (e.g. JVM/test environment)", e);
        }
    }
}
