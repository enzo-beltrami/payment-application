package dev.brenzo.payment_application.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A machine-readable error.")
public record ErrorResponse(

        @Schema(example = "invalid_card_number") String error,
        @Schema(example = "card number failed Mod 10 (Luhn) check") String message,
        @Schema(description = "Field-level validation issues, when applicable.")
        List<FieldError> fieldErrors) {

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, List.of());
    }

    public record FieldError(
            @Schema(example = "cardNumber") String field,
            @Schema(example = "must not be blank") String message) {
    }
}
