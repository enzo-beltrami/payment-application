package dev.brenzo.payment_application.adapter.out.persistence;

import dev.brenzo.payment_application.application.port.out.WebhookDeliveryRepository;
import dev.brenzo.payment_application.domain.webhook.WebhookDelivery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Repository
public class WebhookDeliveryRepositoryAdapter implements WebhookDeliveryRepository {

    private final WebhookDeliveryJpaRepository jpa;

    public WebhookDeliveryRepositoryAdapter(WebhookDeliveryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public WebhookDelivery save(WebhookDelivery delivery) {
        WebhookDeliveryJpaEntity entity = jpa.findById(delivery.id())
                .orElseGet(WebhookDeliveryJpaEntity::new);
        applyDomain(delivery, entity);
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public int claimAndProcessDuePending(int batchSize, Consumer<WebhookDelivery> processor) {
        List<WebhookDeliveryJpaEntity> rows = jpa.claimDuePending(batchSize);
        for (WebhookDeliveryJpaEntity entity : rows) {
            WebhookDelivery domain = toDomain(entity);
            processor.accept(domain);
            applyDomain(domain, entity);
        }
        return rows.size();
    }

    @Override
    public List<WebhookDelivery> findByPaymentId(UUID paymentId) {
        return jpa.findAllByPaymentId(paymentId).stream()
                .map(WebhookDeliveryRepositoryAdapter::toDomain)
                .toList();
    }

    private static void applyDomain(WebhookDelivery d, WebhookDeliveryJpaEntity e) {
        e.setId(d.id());
        e.setPaymentId(d.paymentId());
        e.setSubscriptionId(d.subscriptionId());
        e.setUrl(d.url());
        e.setPayloadJson(d.payloadJson());
        e.setStatus(d.status());
        e.setAttempts(d.attempts());
        e.setNextAttemptAt(d.nextAttemptAt());
        e.setLastResponseCode(d.lastResponseCode());
        e.setLastError(d.lastError());
        e.setCreatedAt(d.createdAt());
        e.setUpdatedAt(d.updatedAt());
    }

    private static WebhookDelivery toDomain(WebhookDeliveryJpaEntity e) {
        return new WebhookDelivery(
                e.getId(),
                e.getPaymentId(),
                e.getSubscriptionId(),
                e.getUrl(),
                e.getPayloadJson(),
                e.getStatus(),
                e.getAttempts(),
                e.getNextAttemptAt(),
                e.getLastResponseCode(),
                e.getLastError(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
