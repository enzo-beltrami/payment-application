package dev.brenzo.payment_application.domain.payment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Payment {

    private final UUID id;
    private final String firstName;
    private final String lastName;
    private final String zipCode;
    private final CardNumber cardNumber;
    private final CardLast4 cardLast4;
    private final Instant createdAt;

    public Payment(UUID id,
                   String firstName,
                   String lastName,
                   String zipCode,
                   CardNumber cardNumber,
                   Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.firstName = requireNonBlank(firstName, "firstName");
        this.lastName = requireNonBlank(lastName, "lastName");
        this.zipCode = requireNonBlank(zipCode, "zipCode");
        this.cardNumber = Objects.requireNonNull(cardNumber, "cardNumber");
        this.cardLast4 = cardNumber.last4();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public UUID id() { return id; }
    public String firstName() { return firstName; }
    public String lastName() { return lastName; }
    public String zipCode() { return zipCode; }
    public CardNumber cardNumber() { return cardNumber; }
    public CardLast4 cardLast4() { return cardLast4; }
    public Instant createdAt() { return createdAt; }
}
