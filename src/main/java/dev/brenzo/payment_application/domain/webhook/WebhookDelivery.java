package dev.brenzo.payment_application.domain.webhook;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class WebhookDelivery {

    private final UUID id;
    private final UUID paymentId;
    private final UUID subscriptionId;
    private final String url;
    private final String payloadJson;
    private final Instant createdAt;

    private DeliveryStatus status;
    private int attempts;
    private Instant nextAttemptAt;
    private Integer lastResponseCode;
    private String lastError;
    private Instant updatedAt;

    public WebhookDelivery(UUID id,
                           UUID paymentId,
                           UUID subscriptionId,
                           String url,
                           String payloadJson,
                           DeliveryStatus status,
                           int attempts,
                           Instant nextAttemptAt,
                           Integer lastResponseCode,
                           String lastError,
                           Instant createdAt,
                           Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.paymentId = Objects.requireNonNull(paymentId);
        this.subscriptionId = Objects.requireNonNull(subscriptionId);
        this.url = Objects.requireNonNull(url);
        this.payloadJson = Objects.requireNonNull(payloadJson);
        this.status = Objects.requireNonNull(status);
        this.attempts = attempts;
        this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt);
        this.lastResponseCode = lastResponseCode;
        this.lastError = lastError;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static WebhookDelivery newPending(UUID id,
                                             UUID paymentId,
                                             UUID subscriptionId,
                                             String url,
                                             String payloadJson,
                                             Instant now) {
        return new WebhookDelivery(
                id, paymentId, subscriptionId, url, payloadJson,
                DeliveryStatus.PENDING, 0, now, null, null, now, now);
    }

    public void markDelivered(Instant now, int responseCode) {
        this.attempts += 1;
        this.lastResponseCode = responseCode;
        this.lastError = null;
        this.status = DeliveryStatus.DELIVERED;
        this.updatedAt = now;
    }

    public void markFailedAndScheduleRetry(Instant now,
                                           Integer responseCode,
                                           String errorMessage,
                                           int maxAttempts,
                                           int backoffCapSeconds) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (backoffCapSeconds < 1) {
            throw new IllegalArgumentException("backoffCapSeconds must be >= 1");
        }
        this.attempts += 1;
        this.lastResponseCode = responseCode;
        this.lastError = errorMessage;
        this.updatedAt = now;
        if (this.attempts >= maxAttempts) {
            this.status = DeliveryStatus.FAILED;
            return;
        }
        this.status = DeliveryStatus.PENDING;
        long backoffSeconds = Math.min(1L << Math.min(this.attempts, 30), backoffCapSeconds);
        this.nextAttemptAt = now.plusSeconds(backoffSeconds);
    }

    public UUID id() { return id; }
    public UUID paymentId() { return paymentId; }
    public UUID subscriptionId() { return subscriptionId; }
    public String url() { return url; }
    public String payloadJson() { return payloadJson; }
    public DeliveryStatus status() { return status; }
    public int attempts() { return attempts; }
    public Instant nextAttemptAt() { return nextAttemptAt; }
    public Integer lastResponseCode() { return lastResponseCode; }
    public String lastError() { return lastError; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
