package dev.brenzo.payment_application.domain.webhook;

import java.time.Instant;
import java.util.UUID;

public record PaymentCreatedEvent(
        UUID deliveryId,
        UUID paymentId,
        String firstName,
        String lastName,
        String zipCode,
        String cardLast4,
        Instant createdAt) {

    public static final String EVENT_TYPE = "payment.created";
}
