package com.velvet.couplesgame.core.letter;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Encrypts sealed letter bodies with an AES-256-GCM key that never leaves the
 * Android Keystore. The key is generated on first use and is not exportable,
 * so letter text cannot be lifted out of the app's own storage.
 *
 * <p>This protects the letter at rest (backups, rooted file access, log
 * scraping). The reveal date itself is enforced by {@link SealedLetterManager};
 * encryption is the second lock, not the only one.
 */
public final class LetterCrypto {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "velvet_letter_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private LetterCrypto() {}

    /**
     * Encrypt a letter body with an AES-256-GCM key that never leaves the
     * Android Keystore.
     *
     * <p>On any failure (keystore unavailable, key generation refused, or
     * cipher error) this method returns {@code null} rather than falling back
     * to plaintext. The caller — {@code SealedLetterManager.seal()} — treats
     * {@code null} as a hard failure and refuses to persist the letter, so an
     * unencrypted body is never written to disk. Do not remove this
     * fail-closed contract: "no secure storage" must mean "no letter saved",
     * not "letter saved in the clear".
     *
     * @return base64 payload (IV + ciphertext + GCM tag), or {@code null} when
     *         encryption is unavailable; callers must not persist the body.
     */
    public static String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            return Base64.encodeToString(payload, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Decrypt a body previously produced by {@link #encrypt(String)}.
     *
     * @return the plaintext, or null when the payload cannot be decrypted.
     */
    public static String decrypt(String encodedPayload) {
        if (encodedPayload == null || encodedPayload.isEmpty()) return null;
        try {
            byte[] payload = Base64.decode(encodedPayload, Base64.NO_WRAP);
            if (payload.length <= IV_BYTES) return null;
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_BYTES);
            byte[] cipherText = new byte[payload.length - IV_BYTES];
            System.arraycopy(payload, IV_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);
        KeyStore.Entry existing = keyStore.getEntry(KEY_ALIAS, null);
        if (existing instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) existing).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
