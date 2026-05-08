package dev.brenzo.payment_application.adapter.in.web.dto;

import dev.brenzo.payment_application.domain.webhook.WebhookSubscription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A registered webhook subscription.")
public record WebhookResponse(

        @Schema(example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(example = "https://example.com/webhooks/payments") String url,
        @Schema(example = "true") boolean active,
        @Schema(example = "2026-05-07T19:53:00Z") Instant createdAt) {

    public static WebhookResponse from(WebhookSubscription s) {
        return new WebhookResponse(s.id(), s.url(), s.active(), s.createdAt());
    }
}
