package dev.brenzo.payment_application.adapter.out.persistence;

import dev.brenzo.payment_application.domain.webhook.DeliveryStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_delivery")
public class WebhookDeliveryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeliveryStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_response_code")
    private Integer lastResponseCode;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WebhookDeliveryJpaEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public Integer getLastResponseCode() { return lastResponseCode; }
    public void setLastResponseCode(Integer lastResponseCode) { this.lastResponseCode = lastResponseCode; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
