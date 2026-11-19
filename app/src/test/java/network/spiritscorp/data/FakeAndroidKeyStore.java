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

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.StrongBoxUnavailableException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * In-memory replacement for the "AndroidKeyStore" JCA provider.
 * <p>
 * Registers a real {@link Provider} named "AndroidKeyStore", so production code that calls
 * {@code KeyStore.getInstance("AndroidKeyStore")} and {@code KeyGenerator.getInstance("AES", "AndroidKeyStore")}
 * runs unchanged. Keys are plain software AES keys, so the default JCA {@code Cipher} works with them
 * and real AES-GCM round trips can be verified.
 * <p>
 * One instance = one fake device keystore. All state is shared between every {@code KeyStore.getInstance} call,
 * exactly like the real keystore.
 */
final class FakeAndroidKeyStore {

    private static final String KEY_ALGORITHM_AES = "AES";
    private static final String PROVIDER_INFO = "In-memory fake of the Android KeyStore for unit tests";
    private static final double PROVIDER_VERSION = 1.0;
    private static final String SERVICE_TYPE_KEYSTORE = "KeyStore";
    private static final String SERVICE_TYPE_KEY_GENERATOR = "KeyGenerator";
    private static final int BITS_PER_BYTE = 8;
    private static final int DEFAULT_KEY_SIZE_BITS = 256;

    private final Map<String, SecretKey> keys = new HashMap<>();
    private final List<KeyGenParameterSpec> generationRequests = new ArrayList<>();
    private final SecureRandom random = new SecureRandom();

    // ---- failure injection (null / false = behave like a healthy keystore) ----

    /** Thrown by KeyStore.load(null). */
    IOException loadFailure;
    /** Thrown by KeyStore.getEntry(...). */
    UnrecoverableEntryException entryFailure;
    /** If true, getEntry(...) returns null even though the alias exists. */
    boolean returnNullEntry;
    /** If true, generating a StrongBox-backed key throws StrongBoxUnavailableException. */
    boolean strongBoxUnavailable;
    /** Thrown by KeyGenerator.init(spec). */
    InvalidAlgorithmParameterException generatorFailure;

    // ---- installation ----

    void install() {
        Security.removeProvider(DatabaseConstants.KEYSTORE_PROVIDER);
        Security.addProvider(new FakeProvider());
    }

    void uninstall() {
        Security.removeProvider(DatabaseConstants.KEYSTORE_PROVIDER);
    }

    // ---- inspection / arrangement helpers for tests ----

    /** Puts a fresh random AES key under the alias (simulates a key that already exists) and returns it. */
    SecretKey putKey(String alias) {
        SecretKey key = newRandomKey(DEFAULT_KEY_SIZE_BITS);
        keys.put(alias, key);
        return key;
    }

    SecretKey getKey(String alias) {
        return keys.get(alias);
    }

    void removeKey(String alias) {
        keys.remove(alias);
    }

    boolean containsAlias(String alias) {
        return keys.containsKey(alias);
    }

    /** Every key generation attempt, including the ones that failed (e.g. the StrongBox attempt). */
    List<KeyGenParameterSpec> getGenerationRequests() {
        return Collections.unmodifiableList(generationRequests);
    }

    private SecretKey newRandomKey(int sizeBits) {
        byte[] raw = new byte[sizeBits / BITS_PER_BYTE];
        random.nextBytes(raw);
        return new SecretKeySpec(raw, KEY_ALGORITHM_AES);
    }

    // ---- JCA plumbing ----

    private final class FakeProvider extends Provider {
        FakeProvider() {
            super(DatabaseConstants.KEYSTORE_PROVIDER, PROVIDER_VERSION, PROVIDER_INFO);
            // Service.newInstance is overridden so the SPIs share this FakeAndroidKeyStore's state
            // (the default reflection-based creation would need a static no-arg constructor).
            putService(new Service(this, SERVICE_TYPE_KEYSTORE, DatabaseConstants.KEYSTORE_PROVIDER,
                    FakeKeyStoreSpi.class.getName(), null, null) {
                @Override
                public Object newInstance(Object constructorParameter) {
                    return new FakeKeyStoreSpi();
                }
            });
            putService(new Service(this, SERVICE_TYPE_KEY_GENERATOR, KEY_ALGORITHM_AES,
                    FakeKeyGeneratorSpi.class.getName(), null, null) {
                @Override
                public Object newInstance(Object constructorParameter) {
                    return new FakeKeyGeneratorSpi();
                }
            });
        }
    }

    private final class FakeKeyGeneratorSpi extends KeyGeneratorSpi {
        private KeyGenParameterSpec pendingSpec;

        @Override
        protected void engineInit(SecureRandom random) {
            throw new UnsupportedOperationException("The production code always passes a KeyGenParameterSpec.");
        }

        @Override
        protected void engineInit(int keySize, SecureRandom random) {
            throw new UnsupportedOperationException("The production code always passes a KeyGenParameterSpec.");
        }

        @Override
        protected void engineInit(AlgorithmParameterSpec params, SecureRandom random)
                throws InvalidAlgorithmParameterException {
            if (generatorFailure != null) {
                throw generatorFailure;
            }
            if (!(params instanceof KeyGenParameterSpec)) {
                throw new InvalidAlgorithmParameterException("KeyGenParameterSpec expected");
            }
            pendingSpec = (KeyGenParameterSpec) params;
        }

        @Override
        protected SecretKey engineGenerateKey() {
            generationRequests.add(pendingSpec);
            if (pendingSpec.isStrongBoxBacked() && strongBoxUnavailable) {
                throw new StrongBoxUnavailableException("StrongBox is not available (fake)");
            }
            SecretKey key = newRandomKey(pendingSpec.getKeySize());
            keys.put(pendingSpec.getKeystoreAlias(), key);
            return key;
        }
    }

    private final class FakeKeyStoreSpi extends KeyStoreSpi {

        @Override
        public void engineLoad(InputStream stream, char[] password) throws IOException {
            if (loadFailure != null) {
                throw loadFailure;
            }
        }

        @Override
        public KeyStore.Entry engineGetEntry(String alias, KeyStore.ProtectionParameter protParam)
                throws UnrecoverableEntryException {
            if (entryFailure != null) {
                throw entryFailure;
            }
            SecretKey key = keys.get(alias);
            if (returnNullEntry || key == null) {
                return null;
            }
            return new KeyStore.SecretKeyEntry(key);
        }

        @Override
        public Key engineGetKey(String alias, char[] password) {
            return keys.get(alias);
        }

        @Override
        public boolean engineContainsAlias(String alias) {
            return keys.containsKey(alias);
        }

        @Override
        public boolean engineIsKeyEntry(String alias) {
            return keys.containsKey(alias);
        }

        @Override
        public boolean engineIsCertificateEntry(String alias) {
            return false;
        }

        @Override
        public int engineSize() {
            return keys.size();
        }

        @Override
        public Enumeration<String> engineAliases() {
            return Collections.enumeration(new ArrayList<>(keys.keySet()));
        }

        @Override
        public void engineDeleteEntry(String alias) {
            keys.remove(alias);
        }

        // ---- not needed by the code under test ----

        @Override
        public Certificate[] engineGetCertificateChain(String alias) {
            return null;
        }

        @Override
        public Certificate engineGetCertificate(String alias) {
            return null;
        }

        @Override
        public Date engineGetCreationDate(String alias) {
            return new Date();
        }

        @Override
        public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain)
                throws KeyStoreException {
            if (!(key instanceof SecretKey)) {
                throw new KeyStoreException("Only secret keys are supported by this fake");
            }
            keys.put(alias, (SecretKey) key);
        }

        @Override
        public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
            throw new KeyStoreException("Not supported by this fake");
        }

        @Override
        public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
            throw new KeyStoreException("Not supported by this fake");
        }

        @Override
        public String engineGetCertificateAlias(Certificate cert) {
            return null;
        }

        @Override
        public void engineStore(OutputStream stream, char[] password) {
            // Nothing to persist: the fake keeps everything in memory.
        }
    }
}
