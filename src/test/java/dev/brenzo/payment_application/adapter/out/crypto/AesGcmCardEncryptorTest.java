package dev.brenzo.payment_application.adapter.out.crypto;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmCardEncryptorTest {

    private AesGcmCardEncryptor newEncryptor() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        EncryptionProperties props = new EncryptionProperties();
        props.setKey(Base64.getEncoder().encodeToString(key));
        return new AesGcmCardEncryptor(props);
    }

    @Test
    void round_trips_plaintext() {
        AesGcmCardEncryptor enc = newEncryptor();
        String pan = "4242424242424242";
        String ct = enc.encrypt(pan);
        assertThat(ct).isNotEqualTo(pan);
        assertThat(enc.decrypt(ct)).isEqualTo(pan);
    }

    @Test
    void produces_different_ciphertexts_for_same_plaintext() {
        AesGcmCardEncryptor enc = newEncryptor();
        String a = enc.encrypt("4242424242424242");
        String b = enc.encrypt("4242424242424242");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rejects_tampered_ciphertext() {
        AesGcmCardEncryptor enc = newEncryptor();
        String ct = enc.encrypt("4242424242424242");
        byte[] bytes = Base64.getDecoder().decode(ct);
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);
        assertThatThrownBy(() -> enc.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejects_missing_key() {
        EncryptionProperties props = new EncryptionProperties();
        assertThatThrownBy(() -> new AesGcmCardEncryptor(props))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejects_invalid_key_length() {
        EncryptionProperties props = new EncryptionProperties();
        props.setKey(Base64.getEncoder().encodeToString(new byte[7]));
        assertThatThrownBy(() -> new AesGcmCardEncryptor(props))
                .isInstanceOf(IllegalStateException.class);
    }
}
