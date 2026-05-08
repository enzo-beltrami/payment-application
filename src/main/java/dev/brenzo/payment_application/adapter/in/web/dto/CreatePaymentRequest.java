package dev.brenzo.payment_application.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for creating a new payment.")
public record CreatePaymentRequest(

        @Schema(example = "Ada", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String firstName,

        @Schema(example = "Lovelace", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String lastName,

        @Schema(example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 20)
        String zipCode,

        @Schema(
                example = "4242424242424242",
                description = "Card number (13–19 digits, must pass Mod 10 / Luhn check). Spaces and hyphens are accepted.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String cardNumber) {
}
