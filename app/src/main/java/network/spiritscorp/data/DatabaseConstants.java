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

public class DatabaseConstants {

    /** No instances of this class should be created. */
    private DatabaseConstants() {}

    /** Primary encrypted SQLite database filename. */
    static final String DATABASE_NAME = "insulin_calculator.db";

    /** SharedPreferences file name for cryptographic key metadata. */
    static final String SECURITY_PREFS_FILE = "secure_db_vault_prefs";

    /** SharedPreferences key for the encrypted database passphrase. */
    static final String PASSPHRASE_ENCRYPTED_KEY = "secure_db_encrypted_passphrase";

    /** SharedPreferences key for the AES initialization vector (IV). */
    static final String PASSPHRASE_IV_KEY = "secure_db_passphrase_iv";

    /** Hardware-backed Android Keystore provider name. */
    static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    /** KeyStore master encryption key alias. */
    static final String SECURITY_KEY_ALIAS = "network.spiritscorp.insulincalc.db_vault_master_key_v2";

    /** Cipher transformation used to encrypt and decrypt the database passphrase. */
    static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    /** Authentication tag length in bits for AES-GCM encryption. */
    static final int GCM_TAG_LENGTH_BITS = 128;

    /** Length in bytes of the randomly generated SQLCipher database passphrase. */
    static final int PASSPHRASE_BYTE_LENGTH = 32;
}
