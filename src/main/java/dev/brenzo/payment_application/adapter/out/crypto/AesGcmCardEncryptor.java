package dev.brenzo.payment_application.adapter.out.crypto;

import dev.brenzo.payment_application.application.port.out.CardEncryptor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmCardEncryptor implements CardEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCardEncryptor(EncryptionProperties properties) {
        if (properties.getKey() == null || properties.getKey().isBlank()) {
            throw new IllegalStateException("app.encryption.key must be configured");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(properties.getKey());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.encryption.key must be Base64-encoded", e);
        }
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.encryption.key must decode to 16, 24, or 32 bytes (AES-128/192/256)");
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buf.put(iv);
            buf.put(cipherBytes);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("encryption failed", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        try {
            byte[] all = Base64.getDecoder().decode(ciphertext);
            if (all.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("ciphertext too short");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH_BYTES);
            byte[] payload = new byte[all.length - IV_LENGTH_BYTES];
            System.arraycopy(all, IV_LENGTH_BYTES, payload, 0, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("decryption failed", e);
        }
    }
}
