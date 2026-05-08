package dev.brenzo.payment_application.adapter.in.web.dto;

import dev.brenzo.payment_application.domain.payment.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A persisted payment. The card number is never returned in full; only the last 4 digits and a masked representation are exposed.")
public record PaymentResponse(

        @Schema(example = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
        UUID id,

        @Schema(example = "Ada") String firstName,
        @Schema(example = "Lovelace") String lastName,
        @Schema(example = "10001") String zipCode,
        @Schema(example = "4242") String cardLast4,
        @Schema(example = "**** **** **** 4242") String maskedCardNumber,
        @Schema(example = "2026-05-07T19:53:00Z") Instant createdAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.id(),
                p.firstName(),
                p.lastName(),
                p.zipCode(),
                p.cardLast4().value(),
                p.cardLast4().masked(),
                p.createdAt());
    }
}
