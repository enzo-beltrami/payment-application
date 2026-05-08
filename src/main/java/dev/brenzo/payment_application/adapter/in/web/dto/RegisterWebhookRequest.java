package dev.brenzo.payment_application.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Payload for registering a new webhook subscription.")
public record RegisterWebhookRequest(

        @Schema(
                example = "https://example.com/webhooks/payments",
                description = "Absolute http or https URL that will receive POSTed payment events.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @URL(message = "invalid url")
        String url) {
}
