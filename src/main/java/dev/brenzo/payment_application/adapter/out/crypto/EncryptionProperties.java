package dev.brenzo.payment_application.adapter.out.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionProperties {

    /**
     * Base64-encoded 256-bit AES key used to encrypt card numbers at rest.
     */
    private String key;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
