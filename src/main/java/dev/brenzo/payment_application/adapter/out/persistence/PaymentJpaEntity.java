package dev.brenzo.payment_application.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class PaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "card_number_encrypted", nullable = false)
    private String cardNumber;

    @Column(name = "card_last_four", nullable = false, length = 4)
    private String cardLastFour;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PaymentJpaEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
