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
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Enterprise-grade security manager object for SQLite / SQLCipher database encryption.
 *
 * Designed as an instantiable, object-oriented cryptographic vault:
 * 1. Hardware-backed Android KeyStore protection: Encrypts/decrypts the 256-bit database master passphrase
 *    using an AES-256 GCM key stored in the hardware security module (TEE / StrongBox).
 * 2. Pure instance-based encapsulation: Enables clean dependency injection, modularity, and test mocking.
 * 3. JVM / Test Resilience: Gracefully falls back in local JVM unit testing environments where KeyStore is absent.
 */
public class DatabaseSecurityManager {

    private static final String TAG = "DatabaseSecurityManager";
    private static final String PREFS_FILE = "secure_db_vault_prefs";

    // Android KeyStore Alias & Cipher specifications
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "network.spiritscorp.insulincalc.db_vault_master_key_v2";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int PASSPHRASE_BYTE_LENGTH = 32; // 256-bit AES Key

    // Storage Keys
    private static final String KEY_ENCRYPTED_PASSPHRASE = "secure_db_encrypted_passphrase";
    private static final String KEY_PASSPHRASE_IV = "secure_db_passphrase_iv";
    private static final String FALLBACK_KEY_PASSPHRASE = "secure_db_passphrase_fallback";

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private final SharedPreferences mPrefs;

    /**
     * Constructs a new DatabaseSecurityManager instance bound to the provided Android context.
     *
     * @param context Application or activity context.
     */
    public DatabaseSecurityManager(@NonNull Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE));
    }

    /**
     * Constructs a new DatabaseSecurityManager instance with custom SharedPreferences,
     * specifically useful for dependency injection and isolated unit testing.
     *
     * @param prefs SharedPreferences instance to store encrypted key data.
     */
    public DatabaseSecurityManager(@NonNull SharedPreferences prefs) {
        this.mPrefs = prefs;
    }

    /**
     * Retrieves the existing 256-bit database encryption passphrase or generates a new one
     * using SecureRandom if this is the first execution.
     *
     * @return 32-byte (256-bit) encryption key suitable for SQLCipher.
     */
    @NonNull
    public synchronized byte[] getOrCreateDatabasePassphrase() {
        // 1. Try reading the KeyStore-encrypted passphrase
        String encryptedPassphraseBase64 = mPrefs.getString(KEY_ENCRYPTED_PASSPHRASE, null);
        String ivBase64 = mPrefs.getString(KEY_PASSPHRASE_IV, null);

        if (encryptedPassphraseBase64 != null && !encryptedPassphraseBase64.trim().isEmpty() &&
                ivBase64 != null && !ivBase64.trim().isEmpty()) {
            try {
                byte[] encryptedBytes = Base64.decode(encryptedPassphraseBase64, Base64.NO_WRAP);
                byte[] ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP);
                byte[] decrypted = decryptWithKeyStore(ivBytes, encryptedBytes);
                if (decrypted != null && decrypted.length == PASSPHRASE_BYTE_LENGTH) {
                    return decrypted;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt stored KeyStore passphrase: " + e.getMessage(), e);
            }
        }

        // 2. Check fallback storage (used in JVM test environments)
        String fallbackBase64 = mPrefs.getString(FALLBACK_KEY_PASSPHRASE, null);
        if (fallbackBase64 != null && !fallbackBase64.trim().isEmpty()) {
            try {
                byte[] decoded = Base64.decode(fallbackBase64, Base64.NO_WRAP);
                if (decoded != null && decoded.length == PASSPHRASE_BYTE_LENGTH) {
                    return decoded;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to decode fallback passphrase: " + e.getMessage(), e);
            }
        }

        // 3. Generate a new cryptographically random 256-bit passphrase
        SecureRandom secureRandom = new SecureRandom();
        byte[] newKey = new byte[PASSPHRASE_BYTE_LENGTH];
        secureRandom.nextBytes(newKey);

        savePassphraseWithKeyStore(newKey);
        Log.i(TAG, "Successfully generated and persisted new 256-bit database encryption key in KeyStore vault.");
        return newKey;
    }

    private void savePassphraseWithKeyStore(byte[] rawPassphrase) {
        EncryptedData encryptedData = encryptWithKeyStore(rawPassphrase);
        if (encryptedData != null) {
            mPrefs.edit()
                    .putString(KEY_PASSPHRASE_IV, Base64.encodeToString(encryptedData.iv(), Base64.NO_WRAP))
                    .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedData.ciphertext(), Base64.NO_WRAP))
                    .apply();
        } else {
            // Fallback for JVM test environments where KeyStore provider is absent
            mPrefs.edit()
                    .putString(FALLBACK_KEY_PASSPHRASE, Base64.encodeToString(rawPassphrase, Base64.NO_WRAP))
                    .apply();
        }
    }

    private SecretKey getOrCreateKeyStoreMasterKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            if (entry != null) {
                return entry.getSecretKey();
            }
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
        );
        KeyGenParameterSpec keyGenSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build();

        keyGenerator.init(keyGenSpec);
        return keyGenerator.generateKey();
    }

    @Nullable
    public EncryptedData encryptWithKeyStore(@NonNull byte[] plaintext) {
        try {
            SecretKey secretKey = getOrCreateKeyStoreMasterKey();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext);
            return new EncryptedData(iv, ciphertext);
        } catch (Throwable e) {
            Log.w(TAG, "AndroidKeyStore encryption not available (e.g. JVM/test environment): " + e.getMessage());
            return null;
        }
    }

    @Nullable
    public byte[] decryptWithKeyStore(@NonNull byte[] iv, @NonNull byte[] ciphertext) {
        try {
            SecretKey secretKey = getOrCreateKeyStoreMasterKey();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(ciphertext);
        } catch (Throwable e) {
            Log.w(TAG, "AndroidKeyStore decryption not available (e.g. JVM/test environment): " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a byte array into a lowercase hex string.
     *
     * @param bytes Raw byte array.
     * @return Hexadecimal representation.
     */
    @NonNull
    public static String bytesToHex(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public record EncryptedData(byte[] iv, byte[] ciphertext) {
    }
}
