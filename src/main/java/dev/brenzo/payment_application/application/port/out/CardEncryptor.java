package dev.brenzo.payment_application.application.port.out;

public interface CardEncryptor {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
