package dev.brenzo.payment_application.adapter.out.persistence;

import dev.brenzo.payment_application.application.port.out.CardEncryptor;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final CardEncryptor encryptor;

    public EncryptedStringConverter(CardEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        return encryptor.encrypt(plaintext);
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        return encryptor.decrypt(ciphertext);
    }
}
